package br.com.tecsus.sigaubs.utils;

import java.nio.charset.StandardCharsets;
import java.util.Set;

public final class DocumentValidationUtils {

    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "12345678", "password", "senha123", "qwerty123", "admin123", "sigaubs");

    private DocumentValidationUtils() {
    }

    public static String digitsOnly(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }

    public static boolean isValidCpf(String value) {
        String cpf = digitsOnly(value);
        if (cpf == null || cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
            return false;
        }
        return cpfDigit(cpf, 9) == Character.digit(cpf.charAt(9), 10)
                && cpfDigit(cpf, 10) == Character.digit(cpf.charAt(10), 10);
    }

    private static int cpfDigit(String cpf, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Character.digit(cpf.charAt(i), 10) * (length + 1 - i);
        }
        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }

    public static boolean isValidCns(String value) {
        String cns = digitsOnly(value);
        if (cns == null || cns.length() != 15 || cns.chars().distinct().count() == 1) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < cns.length(); i++) {
            sum += Character.digit(cns.charAt(i), 10) * (15 - i);
        }
        return sum % 11 == 0;
    }

    public static boolean isValidBrazilianPhone(String value) {
        String phone = digitsOnly(value);
        return phone != null
                && (phone.length() == 10 || phone.length() == 11)
                && phone.charAt(0) != '0'
                && phone.charAt(2) != '0';
    }

    public static boolean isAcceptablePassword(String value, int minimumLength, int maximumLength) {
        if (value == null
                || value.length() < minimumLength
                || value.length() > maximumLength
                || value.getBytes(StandardCharsets.UTF_8).length > 72) {
            return false;
        }
        return !COMMON_PASSWORDS.contains(value.toLowerCase());
    }
}
