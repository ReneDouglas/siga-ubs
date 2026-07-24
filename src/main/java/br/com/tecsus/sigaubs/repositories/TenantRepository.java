package br.com.tecsus.sigaubs.repositories;

import br.com.tecsus.sigaubs.entities.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    @Transactional(readOnly = true)
    Optional<Tenant> findBySlugAndStatus(String slug, String status);

    @Transactional(readOnly = true)
    Optional<Tenant> findByDomainAndStatus(String domain, String status);

    @Transactional(readOnly = true)
    List<Tenant> findAllByStatusOrderBySlugAsc(String status);
}
