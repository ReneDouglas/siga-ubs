package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.SmsUserSearchDTO;
import br.com.tecsus.sigaubs.entities.SystemRole;
import br.com.tecsus.sigaubs.entities.SystemUser;
import br.com.tecsus.sigaubs.entities.Tenant;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.repositories.SystemRoleRepository;
import br.com.tecsus.sigaubs.repositories.SystemUserRepository;
import br.com.tecsus.sigaubs.repositories.TenantRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.tenancy.TenantContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class AdminSmsUserService {

    private final TenantRepository tenantRepository;
    private final SystemUserRepository systemUserRepository;
    private final SystemRoleRepository systemRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;
    private final TenantSessionService tenantSessionService;

    public AdminSmsUserService(TenantRepository tenantRepository,
            SystemUserRepository systemUserRepository,
            SystemRoleRepository systemRoleRepository,
            PasswordEncoder passwordEncoder,
            TransactionTemplate transactionTemplate,
            TenantSessionService tenantSessionService) {
        this.tenantRepository = tenantRepository;
        this.systemUserRepository = systemUserRepository;
        this.systemRoleRepository = systemRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = transactionTemplate;
        this.tenantSessionService = tenantSessionService;
    }

    public Tenant findTenant(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant não encontrado."));
    }

    public Page<SystemUser> findSmsUsers(Long tenantId, SmsUserSearchDTO search, Pageable pageable) {
        Tenant tenant = findTenant(tenantId);
        return withTenantContext(tenant, () -> transactionTemplate.execute(status -> {
            Page<Long> idsPage = systemUserRepository.findIdsByRoleAndFilters(
                        Roles.ROLE_SMS.toString(),
                        normalizeBlank(search != null ? search.getUsername() : null),
                        normalizeBlank(search != null ? search.getName() : null),
                        search != null ? search.getActive() : null,
                        pageable);
            if (!idsPage.hasContent()) {
                return new PageImpl<>(List.of(), pageable, idsPage.getTotalElements());
            }

            List<SystemUser> users = systemUserRepository.findAllByIdIn(idsPage.getContent());
            Map<Long, SystemUser> usersById = users.stream()
                    .collect(Collectors.toMap(SystemUser::getId, Function.identity()));
            List<SystemUser> orderedUsers = idsPage.getContent().stream()
                    .map(usersById::get)
                    .filter(Objects::nonNull)
                    .toList();
            return new PageImpl<>(orderedUsers, pageable, idsPage.getTotalElements());
        }));
    }

    public SystemUser findSmsUser(Long tenantId, Long userId) {
        Tenant tenant = findTenant(tenantId);
        return withTenantContext(tenant, () -> transactionTemplate.execute(status -> {
            SystemUser user = systemUserRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuário SMS não encontrado."));
            requireSmsRole(user);
            return user;
        }));
    }

    public void createSmsUser(Long tenantId, SystemUser systemUser, SystemUserDetails loggedUser) {
        Tenant tenant = findTenant(tenantId);
        withTenantContext(tenant, () -> transactionTemplate.execute(status -> {
            validatePassword(systemUser.getPassword(), systemUser.getConfirmPassword(), true);
            SystemRole smsRole = findSmsRole();
            systemUser.setId(null);
            systemUser.setUsername(requireText(systemUser.getUsername(), "Login obrigatório."));
            systemUser.setPassword(passwordEncoder.encode(systemUser.getPassword()));
            systemUser.setName(requireText(systemUser.getName(), "Nome obrigatório."));
            systemUser.setEmail(requireText(systemUser.getEmail(), "E-mail obrigatório."));
            systemUser.setActive(true);
            systemUser.setBasicHealthUnit(null);
            systemUser.setRoles(Set.of(smsRole));
            systemUser.setTenantId(tenant.getId());
            systemUser.setCreationDate(LocalDateTime.now());
            systemUser.setCreationUser(loggedUser.getUsername());
            systemUserRepository.save(systemUser);
            return null;
        }));
    }

    public void updateSmsUser(Long tenantId, SystemUser systemUser, SystemUserDetails loggedUser) {
        Tenant tenant = findTenant(tenantId);
        withTenantContext(tenant, () -> transactionTemplate.execute(status -> {
            SystemUser persisted = systemUserRepository.findById(systemUser.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Usuário SMS não encontrado."));
            requireSmsRole(persisted);
            persisted.setName(requireText(systemUser.getName(), "Nome obrigatório."));
            persisted.setEmail(requireText(systemUser.getEmail(), "E-mail obrigatório."));
            persisted.setActive(Boolean.TRUE.equals(systemUser.getActive()));
            if (hasText(systemUser.getPassword())) {
                validatePassword(systemUser.getPassword(), systemUser.getConfirmPassword(), false);
                persisted.setPassword(passwordEncoder.encode(systemUser.getPassword()));
            }
            persisted.setUpdateDate(LocalDateTime.now());
            persisted.setUpdateUser(loggedUser.getUsername());
            systemUserRepository.save(persisted);
            if (!Boolean.TRUE.equals(persisted.getActive())) {
                tenantSessionService.expireTenantUserSessions(tenant.getId(), persisted.getUsername());
            }
            return null;
        }));
    }

    public void activateSmsUser(Long tenantId, Long userId, SystemUserDetails loggedUser) {
        updateSmsUserActive(tenantId, userId, true, loggedUser);
    }

    public void deactivateSmsUser(Long tenantId, Long userId, SystemUserDetails loggedUser) {
        updateSmsUserActive(tenantId, userId, false, loggedUser);
    }

    private void updateSmsUserActive(Long tenantId,
            Long userId,
            boolean active,
            SystemUserDetails loggedUser) {
        Tenant tenant = findTenant(tenantId);
        withTenantContext(tenant, () -> transactionTemplate.execute(status -> {
            SystemUser user = systemUserRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuário SMS não encontrado."));
            requireSmsRole(user);
            user.setActive(active);
            user.setUpdateDate(LocalDateTime.now());
            user.setUpdateUser(loggedUser.getUsername());
            systemUserRepository.save(user);
            if (!active) {
                tenantSessionService.expireTenantUserSessions(tenant.getId(), user.getUsername());
            }
            return null;
        }));
    }

    private <T> T withTenantContext(Tenant tenant, Supplier<T> supplier) {
        TenantContextHolder.setTenant(tenant.getId(), tenant.getSlug());
        try {
            return supplier.get();
        } finally {
            TenantContextHolder.clear();
        }
    }

    private SystemRole findSmsRole() {
        return systemRoleRepository.findByRole(Roles.ROLE_SMS.toString())
                .orElseThrow(() -> new IllegalArgumentException("Perfil SMS não cadastrado."));
    }

    private void requireSmsRole(SystemUser user) {
        boolean sms = user.getRoles().stream()
                .anyMatch(role -> Objects.equals(Roles.ROLE_SMS.toString(), role.getRole()));
        if (!sms) {
            throw new IllegalArgumentException("Usuário não possui perfil SMS.");
        }
    }

    private void validatePassword(String password, String confirmation, boolean required) {
        if (!required && !hasText(password)) {
            return;
        }
        if (!hasText(password) || !Objects.equals(password, confirmation)) {
            throw new IllegalArgumentException("As senhas não conferem.");
        }
    }

    private String requireText(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeBlank(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
