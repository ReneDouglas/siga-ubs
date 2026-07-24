package br.com.tecsus.sigaubs.tenancy;

import br.com.tecsus.sigaubs.entities.Tenant;
import br.com.tecsus.sigaubs.repositories.TenantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class TenantResolverService {

    private static final String ACTIVE = "ACTIVE";

    private final TenantRepository tenantRepository;
    private final String baseDomain;

    public TenantResolverService(TenantRepository tenantRepository,
            @Value("${sigaubs.tenancy.base-domain:sigaubs.com.br}") String baseDomain) {
        this.tenantRepository = tenantRepository;
        this.baseDomain = normalizeHost(baseDomain);
    }

    @Transactional(readOnly = true)
    public Optional<Tenant> findActiveBySlug(String slug) {
        String normalizedSlug = normalizeSlug(slug);
        if (normalizedSlug == null) {
            return Optional.empty();
        }
        return tenantRepository.findBySlugAndStatus(normalizedSlug, ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Tenant> findActiveTenants() {
        return tenantRepository.findAllByStatusOrderBySlugAsc(ACTIVE);
    }

    public Optional<String> resolveSlug(String headerSlug, String hostHeader) {
        String explicitSlug = normalizeSlug(headerSlug);
        String hostSlug = resolveSlugFromHost(hostHeader).orElse(null);

        if (explicitSlug != null && hostSlug != null && !explicitSlug.equals(hostSlug)) {
            throw new TenantSlugMismatchException();
        }
        if (explicitSlug != null) {
            return Optional.of(explicitSlug);
        }
        return Optional.ofNullable(hostSlug);
    }

    public boolean isRootHost(String hostHeader) {
        String host = normalizeHost(hostHeader);
        return "localhost".equals(host) || baseDomain.equals(host);
    }

    private Optional<String> resolveSlugFromHost(String hostHeader) {
        String host = normalizeHost(hostHeader);
        if (host == null || isRootHost(host)) {
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

    public static class TenantSlugMismatchException extends RuntimeException {
    }
}
