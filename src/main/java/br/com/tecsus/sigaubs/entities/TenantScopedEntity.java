package br.com.tecsus.sigaubs.entities;

import br.com.tecsus.sigaubs.tenancy.TenantContextHolder;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.hibernate.annotations.TenantId;

@MappedSuperclass
public abstract class TenantScopedEntity {

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    @PrePersist
    @PreUpdate
    void requireTenantContext() {
        TenantContextHolder.getCurrentTenantId()
                .orElseThrow(() -> new IllegalStateException("Contexto de tenant ausente."));
    }
}
