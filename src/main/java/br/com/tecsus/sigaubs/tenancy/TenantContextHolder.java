package br.com.tecsus.sigaubs.tenancy;

import java.util.Optional;

public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CURRENT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void setTenant(Long id, String slug) {
        CURRENT.set(new TenantContext(id, slug));
    }

    public static Optional<TenantContext> getCurrent() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static Optional<Long> getCurrentTenantId() {
        return getCurrent().map(TenantContext::id);
    }

    public static Long getRequiredTenantId() {
        return getCurrentTenantId()
                .orElseThrow(() -> new IllegalStateException("Contexto de tenant ausente."));
    }

    public static String getRequiredTenantSlug() {
        return getCurrent()
                .map(TenantContext::slug)
                .orElseThrow(() -> new IllegalStateException("Contexto de tenant ausente."));
    }

    public static void clear() {
        CURRENT.remove();
    }
}
