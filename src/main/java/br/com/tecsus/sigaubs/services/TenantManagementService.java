package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.TenantSearchDTO;
import br.com.tecsus.sigaubs.dtos.TenantCommandDTO;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.entities.Tenant;
import br.com.tecsus.sigaubs.enums.TenantStatus;
import br.com.tecsus.sigaubs.repositories.TenantRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class TenantManagementService {

    private final TenantRepository tenantRepository;
    private final TenantSessionService tenantSessionService;
    private final String baseDomain;
    private final String adminSubdomain;

    public TenantManagementService(TenantRepository tenantRepository,
            TenantSessionService tenantSessionService,
            @Value("${sigaubs.tenancy.base-domain:sigaubs.com.br}") String baseDomain,
            @Value("${sigaubs.tenancy.admin-subdomain:admin}") String adminSubdomain) {
        this.tenantRepository = tenantRepository;
        this.tenantSessionService = tenantSessionService;
        this.baseDomain = normalizeHost(baseDomain);
        this.adminSubdomain = normalizeSlug(adminSubdomain);
    }

    @Transactional(readOnly = true)
    public Page<Tenant> findTenantsPaginated(TenantSearchDTO search, Pageable pageable) {
        return tenantRepository.findAll(buildSpecification(search), pageable);
    }

    @Transactional(readOnly = true)
    public ResultadoOperacao<Tenant> findById(Long id) {
        return tenantRepository.findById(id)
                .map(ResultadoOperacao::sucesso)
                .orElseGet(() -> ResultadoOperacao.falha("Tenant não encontrado."));
    }

    @CacheEvict(value = "tenants", allEntries = true)
    @Transactional
    public ResultadoOperacao<Tenant> create(Tenant tenant, SystemUserDetails loggedUser) {
        String slug = normalizeSlug(tenant.getSlug());
        if (slug == null) {
            return ResultadoOperacao.falha("Slug inválido.");
        }
        if (isReservedSlug(slug)) {
            return ResultadoOperacao.falha("Slug reservado para o admin global.");
        }
        if (tenantRepository.existsBySlug(slug)) {
            return ResultadoOperacao.falha("Slug já cadastrado.");
        }
        String name = normalizeBlank(tenant.getName());
        if (name == null) {
            return ResultadoOperacao.falha("Nome obrigatório.");
        }

        tenant.setId(null);
        tenant.setSlug(slug);
        tenant.setName(name);
        tenant.setDomain(normalizeDomainOrDefault(tenant.getDomain(), slug));
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setCreationDate(LocalDateTime.now());
        tenant.setCreationUser(loggedUser.getLoginUsername());
        return ResultadoOperacao.sucesso(tenantRepository.save(tenant));
    }

    @CacheEvict(value = "tenants", allEntries = true)
    @Transactional
    public ResultadoOperacao<Tenant> create(TenantCommandDTO command, SystemUserDetails loggedUser) {
        Tenant tenant = new Tenant();
        tenant.setSlug(command.getSlug());
        tenant.setName(command.getName());
        tenant.setDomain(command.getDomain());
        return create(tenant, loggedUser);
    }

    @CacheEvict(value = "tenants", allEntries = true)
    @Transactional
    public ResultadoOperacao<Tenant> update(TenantCommandDTO command, SystemUserDetails loggedUser) {
        Tenant tenant = new Tenant();
        tenant.setId(command.getId());
        tenant.setName(command.getName());
        tenant.setDomain(command.getDomain());
        return update(tenant, loggedUser);
    }

    @CacheEvict(value = "tenants", allEntries = true)
    @Transactional
    public ResultadoOperacao<Tenant> update(Tenant tenant, SystemUserDetails loggedUser) {
        Tenant persisted = tenantRepository.findById(tenant.getId()).orElse(null);
        if (persisted == null) {
            return ResultadoOperacao.falha("Tenant não encontrado.");
        }
        String domain = normalizeDomainOrDefault(tenant.getDomain(), persisted.getSlug());
        if (domain != null && tenantRepository.existsByDomainAndIdNot(domain, persisted.getId())) {
            return ResultadoOperacao.falha("Domínio já cadastrado.");
        }
        String name = normalizeBlank(tenant.getName());
        if (name == null) {
            return ResultadoOperacao.falha("Nome obrigatório.");
        }

        persisted.setName(name);
        persisted.setDomain(domain);
        touch(persisted, loggedUser);
        return ResultadoOperacao.sucesso(tenantRepository.save(persisted));
    }

    @CacheEvict(value = "tenants", allEntries = true)
    @Transactional
    public ResultadoOperacao<Void> disable(Long id, String reason, SystemUserDetails loggedUser) {
        Tenant tenant = tenantRepository.findById(id).orElse(null);
        if (tenant == null) {
            return ResultadoOperacao.falha("Tenant não encontrado.");
        }
        tenant.setStatus(TenantStatus.DISABLED);
        tenant.setDisabledDate(LocalDateTime.now());
        tenant.setDisabledUser(loggedUser.getLoginUsername());
        tenant.setDisabledReason(normalizeBlank(reason));
        tenant.setMaintenanceDate(null);
        tenant.setMaintenanceUser(null);
        tenant.setMaintenanceMessage(null);
        touch(tenant, loggedUser);
        tenantRepository.save(tenant);
        tenantSessionService.expireTenantSessions(tenant.getId());
        return ResultadoOperacao.sucessoSemValor();
    }

    @CacheEvict(value = "tenants", allEntries = true)
    @Transactional
    public ResultadoOperacao<Void> reactivate(Long id, SystemUserDetails loggedUser) {
        Tenant tenant = tenantRepository.findById(id).orElse(null);
        if (tenant == null) {
            return ResultadoOperacao.falha("Tenant não encontrado.");
        }
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setDisabledDate(null);
        tenant.setDisabledUser(null);
        tenant.setDisabledReason(null);
        tenant.setMaintenanceDate(null);
        tenant.setMaintenanceUser(null);
        tenant.setMaintenanceMessage(null);
        touch(tenant, loggedUser);
        tenantRepository.save(tenant);
        return ResultadoOperacao.sucessoSemValor();
    }

    @CacheEvict(value = "tenants", allEntries = true)
    @Transactional
    public ResultadoOperacao<Void> startMaintenance(Long id, String message, SystemUserDetails loggedUser) {
        Tenant tenant = tenantRepository.findById(id).orElse(null);
        if (tenant == null) {
            return ResultadoOperacao.falha("Tenant não encontrado.");
        }
        if (tenant.isDisabled()) {
            return ResultadoOperacao.falha("Tenant desabilitado não pode entrar em manutenção.");
        }
        tenant.setStatus(TenantStatus.MAINTENANCE);
        tenant.setMaintenanceDate(LocalDateTime.now());
        tenant.setMaintenanceUser(loggedUser.getLoginUsername());
        tenant.setMaintenanceMessage(normalizeBlank(message));
        touch(tenant, loggedUser);
        tenantRepository.save(tenant);
        tenantSessionService.expireTenantSessions(tenant.getId());
        return ResultadoOperacao.sucessoSemValor();
    }

    @CacheEvict(value = "tenants", allEntries = true)
    @Transactional
    public ResultadoOperacao<Void> endMaintenance(Long id, SystemUserDetails loggedUser) {
        Tenant tenant = tenantRepository.findById(id).orElse(null);
        if (tenant == null) {
            return ResultadoOperacao.falha("Tenant não encontrado.");
        }
        if (tenant.isDisabled()) {
            return ResultadoOperacao.falha("Tenant desabilitado não pode ser reativado por este fluxo.");
        }
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setMaintenanceDate(null);
        tenant.setMaintenanceUser(null);
        tenant.setMaintenanceMessage(null);
        touch(tenant, loggedUser);
        tenantRepository.save(tenant);
        return ResultadoOperacao.sucessoSemValor();
    }

    @CacheEvict(value = "tenants", allEntries = true)
    @Transactional
    public ResultadoOperacao<Tenant> updateSlug(Long id,
            String newSlug,
            String confirmation,
            SystemUserDetails loggedUser) {

        Tenant tenant = tenantRepository.findById(id).orElse(null);
        if (tenant == null) {
            return ResultadoOperacao.falha("Tenant não encontrado.");
        }
        String normalizedSlug = normalizeSlug(newSlug);
        if (normalizedSlug == null) {
            return ResultadoOperacao.falha("Slug inválido.");
        }
        if (isReservedSlug(normalizedSlug)) {
            return ResultadoOperacao.falha("Slug reservado para o admin global.");
        }
        if (!normalizedSlug.equals(confirmation)) {
            return ResultadoOperacao.falha("Confirmação do slug inválida.");
        }
        if (normalizedSlug.equals(tenant.getSlug())) {
            return ResultadoOperacao.sucesso(tenant);
        }
        if (tenantRepository.existsBySlugAndIdNot(normalizedSlug, tenant.getId())) {
            return ResultadoOperacao.falha("Slug já cadastrado.");
        }

        tenant.setSlug(normalizedSlug);
        tenant.setDomain(buildDefaultDomain(normalizedSlug));
        touch(tenant, loggedUser);
        Tenant savedTenant = tenantRepository.save(tenant);
        tenantSessionService.expireTenantSessions(savedTenant.getId());
        return ResultadoOperacao.sucesso(savedTenant);
    }

    public TenantStatus[] getStatuses() {
        return TenantStatus.values();
    }

    private Specification<Tenant> buildSpecification(TenantSearchDTO search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && hasText(search.getSlug())) {
                predicates.add(cb.like(cb.lower(root.get("slug")), like(search.getSlug())));
            }
            if (search != null && hasText(search.getName())) {
                predicates.add(cb.like(cb.lower(root.get("name")), like(search.getName())));
            }
            if (search != null && hasText(search.getDomain())) {
                predicates.add(cb.like(cb.lower(root.get("domain")), like(search.getDomain())));
            }
            TenantStatus status = parseStatus(search != null ? search.getStatus() : null);
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private TenantStatus parseStatus(String status) {
        if (!hasText(status)) {
            return null;
        }
        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
        for (TenantStatus tenantStatus : TenantStatus.values()) {
            if (tenantStatus.name().equals(normalizedStatus)) {
                return tenantStatus;
            }
        }
        return null;
    }

    private boolean isReservedSlug(String slug) {
        return slug.equals(adminSubdomain);
    }

    private String normalizeSlug(String value) {
        if (!hasText(value)) {
            return null;
        }
        String slug = value.trim().toLowerCase(Locale.ROOT);
        return slug.matches("[a-z0-9]([a-z0-9-]*[a-z0-9])?") ? slug : null;
    }

    private String normalizeDomainOrDefault(String domain, String slug) {
        String normalizedDomain = normalizeHost(domain);
        return normalizedDomain != null ? normalizedDomain : buildDefaultDomain(slug);
    }

    private String buildDefaultDomain(String slug) {
        return slug + "." + baseDomain;
    }

    private String normalizeHost(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.split(":")[0].trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String like(String value) {
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private String normalizeBlank(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private void touch(Tenant tenant, SystemUserDetails loggedUser) {
        tenant.setUpdateDate(LocalDateTime.now());
        tenant.setUpdateUser(loggedUser.getLoginUsername());
    }
}
