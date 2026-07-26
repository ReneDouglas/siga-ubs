package br.com.tecsus.sigaubs.repositories;

import br.com.tecsus.sigaubs.entities.Tenant;
import br.com.tecsus.sigaubs.enums.TenantStatus;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long>, JpaSpecificationExecutor<Tenant> {

    @Transactional(readOnly = true)
    Optional<Tenant> findBySlug(String slug);

    @Transactional(readOnly = true)
    Optional<Tenant> findBySlugAndStatus(String slug, TenantStatus status);

    default Optional<Tenant> findBySlugAndStatus(String slug, String status) {
        return findBySlugAndStatus(slug, TenantStatus.valueOf(status));
    }

    @Transactional(readOnly = true)
    Optional<Tenant> findByDomainAndStatus(String domain, TenantStatus status);

    default Optional<Tenant> findByDomainAndStatus(String domain, String status) {
        return findByDomainAndStatus(domain, TenantStatus.valueOf(status));
    }

    @Transactional(readOnly = true)
    List<Tenant> findAllByStatusOrderBySlugAsc(TenantStatus status);

    default List<Tenant> findAllByStatusOrderBySlugAsc(String status) {
        return findAllByStatusOrderBySlugAsc(TenantStatus.valueOf(status));
    }

    @Transactional(readOnly = true)
    boolean existsBySlug(String slug);

    @Transactional(readOnly = true)
    boolean existsBySlugAndIdNot(String slug, Long id);

    @Transactional(readOnly = true)
    boolean existsByDomainAndIdNot(String domain, Long id);
}
