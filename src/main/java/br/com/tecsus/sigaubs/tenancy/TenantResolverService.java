package br.com.tecsus.sigaubs.tenancy;

import br.com.tecsus.sigaubs.config.CacheNames;
import br.com.tecsus.sigaubs.entities.Tenant;
import br.com.tecsus.sigaubs.enums.TenantStatus;
import br.com.tecsus.sigaubs.repositories.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class TenantResolverService {

    private final TenantRepository tenantRepository;
    private final String baseDomain;
    private final String adminSubdomain;

    @Autowired
    public TenantResolverService(TenantRepository tenantRepository,
            @Value("${sigaubs.tenancy.base-domain:sigaubs.com.br}") String baseDomain,
            @Value("${sigaubs.tenancy.admin-subdomain:admin}") String adminSubdomain) {
        this.tenantRepository = tenantRepository;
        this.baseDomain = normalizeHost(baseDomain);
        this.adminSubdomain = normalizeSlug(adminSubdomain);
    }

    public TenantResolverService(TenantRepository tenantRepository, String baseDomain) {
        this(tenantRepository, baseDomain, "admin");
    }

    @Transactional(readOnly = true)
    public Optional<Tenant> findActiveBySlug(String slug) {
        String normalizedSlug = normalizeSlug(slug);
        if (normalizedSlug == null) {
            return Optional.empty();
        }
        return tenantRepository.findBySlugAndStatus(normalizedSlug, TenantStatus.ACTIVE.name());
    }

    @Cacheable(
            value = CacheNames.TENANTS,
            key = "'context:' + (#slug == null ? '' : #slug.trim().toLowerCase(T(java.util.Locale).ROOT))",
            unless = "#result == null")
    @Transactional(readOnly = true)
    public Optional<TenantContext> findContextBySlug(String slug) {
        String normalizedSlug = normalizeSlug(slug);
        if (normalizedSlug == null) {
            return Optional.empty();
        }
        return tenantRepository.findBySlug(normalizedSlug)
                .map(tenant -> new TenantContext(
                        tenant.getId(),
                        tenant.getSlug(),
                        tenant.getStatus(),
                        tenant.getMaintenanceMessage()));
    }

    @Cacheable(
            value = CacheNames.TENANTS,
            key = "'active:' + (#slug == null ? '' : #slug.trim().toLowerCase(T(java.util.Locale).ROOT))",
            unless = "#result == null")
    @Transactional(readOnly = true)
    public Optional<TenantContext> findActiveContextBySlug(String slug) {
        return findActiveBySlug(slug)
                .map(tenant -> new TenantContext(
                        tenant.getId(),
                        tenant.getSlug(),
                        tenant.getStatus(),
                        tenant.getMaintenanceMessage()));
    }

    @Transactional(readOnly = true)
    public List<Tenant> findActiveTenants() {
        return tenantRepository.findAllByStatusOrderBySlugAsc(TenantStatus.ACTIVE);
    }

    public SlugResolution resolveSlug(String headerSlug, String hostHeader) {
        if (isAdminHost(hostHeader)) {
            return SlugResolution.notFound();
        }

        String explicitSlug = normalizeSlug(headerSlug);
        String hostSlug = resolveSlugFromHost(hostHeader).orElse(null);

        if (explicitSlug != null && hostSlug != null && !explicitSlug.equals(hostSlug)) {
            return SlugResolution.mismatchFound();
        }
        if (explicitSlug != null) {
            return SlugResolution.found(explicitSlug);
        }
        return hostSlug != null ? SlugResolution.found(hostSlug) : SlugResolution.notFound();
    }

    public boolean isRootHost(String hostHeader) {
        String host = normalizeHost(hostHeader);
        return "localhost".equals(host) || baseDomain.equals(host);
    }

    public boolean isAdminHost(String hostHeader) {
        String host = normalizeHost(hostHeader);
        if (host == null || adminSubdomain == null) {
            return false;
        }
        return host.equals(adminSubdomain + ".localhost")
                || host.equals(adminSubdomain + "." + baseDomain);
    }

    private Optional<String> resolveSlugFromHost(String hostHeader) {
        String host = normalizeHost(hostHeader);
        if (host == null || isRootHost(host) || isAdminHost(host)) {
            return Optional.empty();
        }
        if (host.endsWith(".localhost")) {
            return Optional.ofNullable(normalizeSlug(host.substring(0, host.length() - ".localhost".length())));
        }
        String suffix = "." + baseDomain;
        if (!host.endsWith(suffix)) {
            return Optional.empty();
        }
        return Optional.ofNullable(normalizeSlug(host.substring(0, host.length() - suffix.length())));
    }

    private String normalizeHost(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.split(":")[0].trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeSlug(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String slug = value.trim().toLowerCase(Locale.ROOT);
        return slug.matches("[a-z0-9]([a-z0-9-]*[a-z0-9])?") ? slug : null;
    }

    public record SlugResolution(Optional<String> slug, boolean slugMismatch) {

        static SlugResolution found(String slug) {
            return new SlugResolution(Optional.of(slug), false);
        }

        static SlugResolution notFound() {
            return new SlugResolution(Optional.empty(), false);
        }

        static SlugResolution mismatchFound() {
            return new SlugResolution(Optional.empty(), true);
        }

        public boolean mismatch() {
            return slugMismatch;
        }
    }
}
