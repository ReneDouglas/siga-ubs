package br.com.tecsus.sigaubs.security;

import br.com.tecsus.sigaubs.config.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public final class SessionMetadata {

    public static final String MANAGEMENT_ID_ATTRIBUTE = "SIGAUBS_SESSION_MANAGEMENT_ID";
    public static final String CLIENT_DESCRIPTION_ATTRIBUTE = "SIGAUBS_SESSION_CLIENT_DESCRIPTION";
    public static final String LOCATION_DESCRIPTION_ATTRIBUTE =
            "SIGAUBS_SESSION_LOCATION_DESCRIPTION";
    public static final String LOCATION_CITY_HEADER = "X-SIGAUBS-Location-City";
    public static final String LOCATION_REGION_CODE_HEADER =
            "X-SIGAUBS-Location-Region-Code";
    public static final String LOCATION_REGION_HEADER = "X-SIGAUBS-Location-Region";
    public static final String LOCATION_COUNTRY_HEADER = "X-SIGAUBS-Location-Country";
    public static final String LOCATION_UNAVAILABLE = "Localidade indisponível";
    public static final String LOCAL_DEVELOPMENT_LOCATION =
            "Rede local · Desenvolvimento";
    public static final String CLIENT_UNIDENTIFIED = "Cliente não identificado";

    private static final String TOR_COUNTRY_CODE = "T1";
    private static final String UNKNOWN_COUNTRY_CODE = "XX";
    private static final int MAXIMUM_LOCATION_PART_LENGTH = 80;
    private static final int MAXIMUM_REGION_CODE_LENGTH = 12;
    private static final int MAXIMUM_LOCATION_DESCRIPTION_LENGTH = 128;
    private static final int MAXIMUM_ASCII_CODE_POINT = 0x7F;
    private static final int MAXIMUM_LATIN_1_CODE_POINT = 0xFF;
    private static final Pattern UNSAFE_LOCATION_CHARACTERS =
            Pattern.compile("[^\\p{L}\\p{N}\\p{Zs}.,'’()-]");
    private static final Pattern REPEATED_WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern REGION_CODE = Pattern.compile(
            "[\\p{L}\\p{N}-]{1," + MAXIMUM_REGION_CODE_LENGTH + "}");

    private SessionMetadata() {
    }

    public static String newManagementId() {
        return UUID.randomUUID().toString();
    }

    public static String describeClient(HttpServletRequest request) {
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        if (userAgent == null || userAgent.isBlank()) {
            return CLIENT_UNIDENTIFIED;
        }

        String normalized = userAgent.toLowerCase(Locale.ROOT);
        String browser;
        if (normalized.contains("edg/")) {
            browser = "Edge";
        } else if (normalized.contains("firefox/")) {
            browser = "Firefox";
        } else if (normalized.contains("chrome/") || normalized.contains("crios/")) {
            browser = "Chrome";
        } else if (normalized.contains("safari/")) {
            browser = "Safari";
        } else {
            browser = "Outro navegador";
        }

        boolean mobile = normalized.contains("mobile")
                || normalized.contains("android")
                || normalized.contains("iphone")
                || normalized.contains("ipad");
        return browser + (mobile ? " · dispositivo móvel" : " · computador");
    }

    public static String describeLocation(
            HttpServletRequest request,
            SecurityProperties.SessionLocationSource source) {
        if (source == SecurityProperties.SessionLocationSource.LOCAL) {
            return LOCAL_DEVELOPMENT_LOCATION;
        }
        if (source != SecurityProperties.SessionLocationSource.TRUSTED_PROXY) {
            return LOCATION_UNAVAILABLE;
        }

        String country = normalizeCountry(request.getHeader(LOCATION_COUNTRY_HEADER));
        if (TOR_COUNTRY_CODE.equals(country)) {
            return "Rede Tor · localidade indisponível";
        }
        if (UNKNOWN_COUNTRY_CODE.equals(country)) {
            return LOCATION_UNAVAILABLE;
        }

        String city = normalizeLocationPart(
                request.getHeader(LOCATION_CITY_HEADER), MAXIMUM_LOCATION_PART_LENGTH);
        String regionCode = normalizeRegionCode(
                request.getHeader(LOCATION_REGION_CODE_HEADER));
        String region = regionCode != null
                ? regionCode
                : normalizeLocationPart(
                        request.getHeader(LOCATION_REGION_HEADER),
                        MAXIMUM_LOCATION_PART_LENGTH);

        StringBuilder description = new StringBuilder();
        if (city != null) {
            description.append(city);
        }
        if (region != null && !region.equalsIgnoreCase(city)) {
            if (!description.isEmpty()) {
                description.append(", ");
            }
            description.append(region);
        }
        if (country != null) {
            if (!description.isEmpty()) {
                description.append(" · ");
            }
            description.append(country);
        }
        if (description.isEmpty()) {
            return LOCATION_UNAVAILABLE;
        }
        return truncate(description.toString(), MAXIMUM_LOCATION_DESCRIPTION_LENGTH);
    }

    private static String normalizeCountry(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        return normalized.matches("[A-Z]{2}|T1") ? normalized : null;
    }

    private static String normalizeRegionCode(String value) {
        String normalized = normalizeLocationPart(value, MAXIMUM_REGION_CODE_LENGTH);
        if (normalized == null
                || !REGION_CODE.matcher(normalized).matches()) {
            return null;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private static String normalizeLocationPart(String value, int maximumLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String decoded = decodeUtf8Header(value);
        String normalized = Normalizer.normalize(decoded, Normalizer.Form.NFKC);
        normalized = UNSAFE_LOCATION_CHARACTERS.matcher(normalized).replaceAll(" ");
        normalized = REPEATED_WHITESPACE.matcher(normalized).replaceAll(" ").strip();
        if (normalized.isEmpty()) {
            return null;
        }
        return truncate(normalized, maximumLength);
    }

    private static String decodeUtf8Header(String value) {
        if (value.chars().noneMatch(character -> character > MAXIMUM_ASCII_CODE_POINT)
                || value.chars().anyMatch(
                        character -> character > MAXIMUM_LATIN_1_CODE_POINT)) {
            return value;
        }
        byte[] bytes = value.getBytes(StandardCharsets.ISO_8859_1);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            return value;
        }
    }

    private static String truncate(String value, int maximumLength) {
        if (value.codePointCount(0, value.length()) <= maximumLength) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maximumLength)).strip();
    }
}
