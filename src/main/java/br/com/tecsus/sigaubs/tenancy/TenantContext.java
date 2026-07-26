package br.com.tecsus.sigaubs.tenancy;

import br.com.tecsus.sigaubs.enums.TenantStatus;

public record TenantContext(Long id, String slug, TenantStatus status, String maintenanceMessage) {

    public TenantContext(Long id, String slug) {
        this(id, slug, TenantStatus.ACTIVE, null);
    }

    public boolean isActive() {
        return TenantStatus.ACTIVE.equals(status);
    }

    public boolean isMaintenance() {
        return TenantStatus.MAINTENANCE.equals(status);
    }

    public boolean isDisabled() {
        return TenantStatus.DISABLED.equals(status);
    }
}
