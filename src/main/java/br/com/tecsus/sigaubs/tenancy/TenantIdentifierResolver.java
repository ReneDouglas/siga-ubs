package br.com.tecsus.sigaubs.tenancy;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long> {

    static final Long NO_TENANT = 0L;

    @Override
    public Long resolveCurrentTenantIdentifier() {
        return TenantContextHolder.getCurrentTenantId().orElse(NO_TENANT);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public boolean isRoot(Long tenantId) {
        return false;
    }
}
