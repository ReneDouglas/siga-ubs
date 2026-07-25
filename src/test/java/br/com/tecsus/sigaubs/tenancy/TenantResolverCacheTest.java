package br.com.tecsus.sigaubs.tenancy;

import br.com.tecsus.sigaubs.entities.Tenant;
import br.com.tecsus.sigaubs.repositories.TenantRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.cache.type=caffeine",
        "spring.cache.caffeine.spec=maximumSize=10,expireAfterWrite=5m"
})
@ActiveProfiles("test")
@Transactional
class TenantResolverCacheTest {

    private final TenantRepository tenantRepository;
    private final TenantResolverService tenantResolverService;
    private final EntityManager entityManager;
    private final EntityManagerFactory entityManagerFactory;
    private final CacheManager cacheManager;

    @Autowired
    TenantResolverCacheTest(TenantRepository tenantRepository,
            TenantResolverService tenantResolverService,
            EntityManager entityManager,
            EntityManagerFactory entityManagerFactory,
            CacheManager cacheManager) {
        this.tenantRepository = tenantRepository;
        this.tenantResolverService = tenantResolverService;
        this.entityManager = entityManager;
        this.entityManagerFactory = entityManagerFactory;
        this.cacheManager = cacheManager;
    }

    @AfterEach
    void tearDown() {
        var tenantsCache = cacheManager.getCache("tenants");
        if (tenantsCache != null) {
            tenantsCache.clear();
        }
    }

    @Test
    void deveCachearContextoDoTenantPorSlugNormalizado() {
        Tenant tenant = new Tenant();
        tenant.setSlug("afogados");
        tenant.setName("Afogados");
        tenant.setDomain("afogados.sigaubs.com.br");
        tenant.setStatus("ACTIVE");
        tenant.setCreationDate(LocalDateTime.now());
        tenant.setCreationUser("test");
        tenant = tenantRepository.saveAndFlush(tenant);

        entityManager.clear();
        statistics().clear();

        assertThat(tenantResolverService.findActiveContextBySlug(" Afogados "))
                .contains(new TenantContext(tenant.getId(), "afogados"));
        assertThat(tenantResolverService.findActiveContextBySlug("afogados"))
                .contains(new TenantContext(tenant.getId(), "afogados"));

        assertThat(statistics().getPrepareStatementCount()).isEqualTo(1);
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }
}
