package br.com.tecsus.sigaubs.security;

public final class SessionPrincipalKey {

    private static final String ADMIN_PREFIX = "admin:";
    private static final String TENANT_PREFIX = "tenant:";
    private static final String USER_SEGMENT = ":user:";
    private static final String SQL_WILDCARD = "%";

    private SessionPrincipalKey() {
    }

    public static String forAdmin(Long userId) {
        return userId == null ? null : ADMIN_PREFIX + userId;
    }

    public static String forTenantUser(Long tenantId, Long userId) {
        if (tenantId == null || userId == null) {
            return null;
        }
        return TENANT_PREFIX + tenantId + USER_SEGMENT + userId;
    }

    public static String forTenantUsersLike(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        return TENANT_PREFIX + tenantId + USER_SEGMENT + SQL_WILDCARD;
    }

    public static String forAllTenantUsersLike() {
        return TENANT_PREFIX + SQL_WILDCARD;
    }

    public static boolean isApplicationKey(String principalKey) {
        return principalKey != null
                && (principalKey.startsWith(ADMIN_PREFIX)
                        || principalKey.startsWith(TENANT_PREFIX));
    }
}
