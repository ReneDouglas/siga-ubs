package br.com.tecsus.sigaubs.utils;

public final class AutocompletePolicy {

    public static final int MAXIMUM_RESULTS = 5;
    private static final int MINIMUM_PATIENT_TERM_LENGTH = 4;
    private static final int MINIMUM_USER_TERM_LENGTH = 3;

    private AutocompletePolicy() {
    }

    public static boolean isPatientTermTooShort(String term) {
        return term.length() < MINIMUM_PATIENT_TERM_LENGTH;
    }

    public static boolean isUserTermTooShort(String term) {
        return term.length() < MINIMUM_USER_TERM_LENGTH;
    }
}
