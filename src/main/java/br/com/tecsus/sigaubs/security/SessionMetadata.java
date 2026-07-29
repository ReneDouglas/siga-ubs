package br.com.tecsus.sigaubs.security;

import br.com.tecsus.sigaubs.config.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;

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

    private static final Pattern UNSAFE_LOCATION_CHARACTERS =
            Pattern.compile("[^\\p{L}\\p{N}\\p{Zs}.,'’()-]");
    private static final Pattern REPEATED_WHITESPACE = Pattern.compile("\\s+");

    private SessionMetadata() {
    }

    public static String newManagementId() {
        return UUID.randomUUID().toString();
    }

    public static String describeClient(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            return "Cliente não identificado";
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
        if ("T1".equals(country)) {
            return "Rede Tor · localidade indisponível";
        }
        if ("XX".equals(country)) {
            return LOCATION_UNAVAILABLE;
        }

        String city = normalizeLocationPart(
                request.getHeader(LOCATION_CITY_HEADER), 80);
        String regionCode = normalizeRegionCode(
                request.getHeader(LOCATION_REGION_CODE_HEADER));
        String region = regionCode != null
                ? regionCode
                : normalizeLocationPart(request.getHeader(LOCATION_REGION_HEADER), 80);

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
        return truncate(description.toString(), 128);
    }

    private static String normalizeCountry(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        return normalized.matches("[A-Z]{2}|T1") ? normalized : null;
    }

    private static String normalizeRegionCode(String value) {
        String normalized = normalizeLocationPart(value, 12);
        if (normalized == null
                || !normalized.matches("[\\p{L}\\p{N}-]{1,12}")) {
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
        if (value.chars().noneMatch(character -> character > 0x7F)
                || value.chars().anyMatch(character -> character > 0xFF)) {
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
