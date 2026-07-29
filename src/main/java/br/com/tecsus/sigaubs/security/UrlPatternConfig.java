package br.com.tecsus.sigaubs.security;

public final class UrlPatternConfig {

    public static final String[] PUBLIC_MATCHERS = {
            "/css/**",
            "/images/**",
            "/js/**",
            "/vendor/**",
            "/login",
            "/login-error",
            "/logout",
            "/error",
            "/expired",
            "/maintenance",
            "/webjars/**",
            "/favicon.ico",
            /*"/actuator/**"*/
    };

    public static final String[] PRIVATE_MATCHERS = {
            "/",
            "/systemUser-management/**",
            "/basicHealthUnit-management/**",
            "/patient-management/**",
            "/patient-list/**",
            "/appointment-management/**",
            "/specialty-management/**",
            "/medicalSlot-management/**",
            "/contemplation-management/**",
            "/queue-management/**",
    };

}
