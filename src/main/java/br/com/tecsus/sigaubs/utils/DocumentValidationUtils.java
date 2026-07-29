package br.com.tecsus.sigaubs.utils;

import java.nio.charset.StandardCharsets;
import java.util.Set;

public final class DocumentValidationUtils {

    private static final int DECIMAL_RADIX = 10;
    private static final int MODULO_ELEVEN = 11;
    private static final int CPF_LENGTH = 11;
    private static final int CPF_FIRST_CHECK_DIGIT_INDEX = 9;
    private static final int CPF_SECOND_CHECK_DIGIT_INDEX = 10;
    private static final int CNS_LENGTH = 15;
    private static final int LANDLINE_PHONE_LENGTH = 10;
    private static final int MOBILE_PHONE_LENGTH = 11;
    private static final int AREA_CODE_START_INDEX = 0;
    private static final int SUBSCRIBER_NUMBER_START_INDEX = 2;
    private static final int MAXIMUM_BCRYPT_PASSWORD_BYTES = 72;
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "12345678", "password", "senha123", "qwerty123", "admin123", "sigaubs");

    private DocumentValidationUtils() {
    }

    public static String digitsOnly(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }

    public static boolean isValidCpf(String value) {
        String cpf = digitsOnly(value);
        if (cpf == null
                || cpf.length() != CPF_LENGTH
                || cpf.chars().distinct().count() == 1) {
            return false;
        }
        return cpfDigit(cpf, CPF_FIRST_CHECK_DIGIT_INDEX)
                        == Character.digit(
                                cpf.charAt(CPF_FIRST_CHECK_DIGIT_INDEX), DECIMAL_RADIX)
                && cpfDigit(cpf, CPF_SECOND_CHECK_DIGIT_INDEX)
                        == Character.digit(
                                cpf.charAt(CPF_SECOND_CHECK_DIGIT_INDEX), DECIMAL_RADIX);
    }

    private static int cpfDigit(String cpf, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Character.digit(cpf.charAt(i), DECIMAL_RADIX) * (length + 1 - i);
        }
        int remainder = (sum * DECIMAL_RADIX) % MODULO_ELEVEN;
        return remainder == DECIMAL_RADIX ? 0 : remainder;
    }

    public static boolean isValidCns(String value) {
        String cns = digitsOnly(value);
        if (cns == null
                || cns.length() != CNS_LENGTH
                || cns.chars().distinct().count() == 1) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < cns.length(); i++) {
            sum += Character.digit(cns.charAt(i), DECIMAL_RADIX) * (CNS_LENGTH - i);
        }
        return sum % MODULO_ELEVEN == 0;
    }

    public static boolean isValidBrazilianPhone(String value) {
        String phone = digitsOnly(value);
        return phone != null
                && (phone.length() == LANDLINE_PHONE_LENGTH
                        || phone.length() == MOBILE_PHONE_LENGTH)
                && phone.charAt(AREA_CODE_START_INDEX) != '0'
                && phone.charAt(SUBSCRIBER_NUMBER_START_INDEX) != '0';
    }

    public static boolean isAcceptablePassword(String value, int minimumLength, int maximumLength) {
        if (value == null
                || value.length() < minimumLength
                || value.length() > maximumLength
                || value.getBytes(StandardCharsets.UTF_8).length
                        > MAXIMUM_BCRYPT_PASSWORD_BYTES) {
            return false;
        }
        return !COMMON_PASSWORDS.contains(value.toLowerCase());
    }
}
