package br.com.tecsus.sigaubs.utils;

public final class LogValueSanitizer {

    private static final int MAX_LOG_VALUE_LENGTH = 256;

    private LogValueSanitizer() {
    }

    public static String sanitize(Object value) {
        if (value == null) {
            return "<null>";
        }
        String raw = String.valueOf(value);
        StringBuilder sanitized = new StringBuilder(Math.min(raw.length(), MAX_LOG_VALUE_LENGTH));
        for (int i = 0; i < raw.length() && sanitized.length() < MAX_LOG_VALUE_LENGTH; i++) {
            char current = raw.charAt(i);
            sanitized.append(Character.isISOControl(current) ? '_' : current);
        }
        return sanitized.toString();
    }
}
