package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.SmsUserSearchDTO;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.dtos.AdminAccountCommandDTO;
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
import org.springframework.beans.factory.annotation.Autowired;
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
    private final PasswordPolicyService passwordPolicyService;

    @Autowired
    public AdminSmsUserService(TenantRepository tenantRepository,
            SystemUserRepository systemUserRepository,
            SystemRoleRepository systemRoleRepository,
            PasswordEncoder passwordEncoder,
            TransactionTemplate transactionTemplate,
            TenantSessionService tenantSessionService,
            PasswordPolicyService passwordPolicyService) {
        this.tenantRepository = tenantRepository;
        this.systemUserRepository = systemUserRepository;
        this.systemRoleRepository = systemRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = transactionTemplate;
        this.tenantSessionService = tenantSessionService;
        this.passwordPolicyService = passwordPolicyService;
    }

    AdminSmsUserService(TenantRepository tenantRepository,
            SystemUserRepository systemUserRepository,
            SystemRoleRepository systemRoleRepository,
            PasswordEncoder passwordEncoder,
            TransactionTemplate transactionTemplate,
            TenantSessionService tenantSessionService) {
        this(tenantRepository, systemUserRepository, systemRoleRepository, passwordEncoder,
                transactionTemplate, tenantSessionService, null);
    }

    public ResultadoOperacao<Void> createSmsUser(Long tenantId,
            AdminAccountCommandDTO command,
            SystemUserDetails loggedUser) {
        return createSmsUser(tenantId, toSystemUser(command), loggedUser);
    }

    public ResultadoOperacao<Void> updateSmsUser(Long tenantId,
            AdminAccountCommandDTO command,
            SystemUserDetails loggedUser) {
        return updateSmsUser(tenantId, toSystemUser(command), loggedUser);
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
            var roleResult = validateSmsRole(user);
            if (roleResult.falhou()) {
                throw new IllegalArgumentException(roleResult.mensagem());
            }
            return user;
        }));
    }

    public ResultadoOperacao<Void> createSmsUser(Long tenantId, SystemUser systemUser,
            SystemUserDetails loggedUser) {
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            return ResultadoOperacao.falha("Tenant não encontrado.");
        }
        return withTenantContext(tenant, () -> transactionTemplate.execute(status -> {
            var passwordResult = validatePassword(systemUser.getPassword(), systemUser.getConfirmPassword(), true);
            if (passwordResult.falhou()) {
                return passwordResult;
            }
            var roleResult = findSmsRole();
            if (roleResult.falhou()) {
                return ResultadoOperacao.<Void>falha(roleResult.mensagem());
            }
            String username = requireText(systemUser.getUsername(), "Login obrigatório.");
            if (username == null) {
                return ResultadoOperacao.falha("Login obrigatório.");
            }
            String name = requireText(systemUser.getName(), "Nome obrigatório.");
            if (name == null) {
                return ResultadoOperacao.falha("Nome obrigatório.");
            }
            String email = requireText(systemUser.getEmail(), "E-mail obrigatório.");
            if (email == null) {
                return ResultadoOperacao.falha("E-mail obrigatório.");
            }
            SystemRole smsRole = roleResult.valor();
            systemUser.setId(null);
            systemUser.setUsername(username);
            systemUser.setPassword(passwordEncoder.encode(systemUser.getPassword()));
            systemUser.setName(name);
            systemUser.setEmail(email);
            systemUser.setActive(true);
            systemUser.setBasicHealthUnit(null);
            systemUser.setRoles(Set.of(smsRole));
            systemUser.setTenantId(tenant.getId());
            systemUser.setCreationDate(LocalDateTime.now());
            systemUser.setCreationUser(loggedUser.getLoginUsername());
            systemUserRepository.save(systemUser);
            return ResultadoOperacao.sucessoSemValor();
        }));
    }

    public ResultadoOperacao<Void> updateSmsUser(Long tenantId, SystemUser systemUser,
            SystemUserDetails loggedUser) {
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            return ResultadoOperacao.falha("Tenant não encontrado.");
        }
        return withTenantContext(tenant, () -> transactionTemplate.execute(status -> {
            SystemUser persisted = systemUserRepository.findById(systemUser.getId())
                    .orElse(null);
            if (persisted == null) {
                return ResultadoOperacao.falha("Usuário SMS não encontrado.");
            }
            var roleResult = validateSmsRole(persisted);
            if (roleResult.falhou()) {
                return roleResult;
            }
            String name = requireText(systemUser.getName(), "Nome obrigatório.");
            if (name == null) {
                return ResultadoOperacao.falha("Nome obrigatório.");
            }
            String email = requireText(systemUser.getEmail(), "E-mail obrigatório.");
            if (email == null) {
                return ResultadoOperacao.falha("E-mail obrigatório.");
            }
            boolean passwordChanged = hasText(systemUser.getPassword());
            if (passwordChanged) {
                var passwordResult = validatePassword(systemUser.getPassword(), systemUser.getConfirmPassword(), false);
                if (passwordResult.falhou()) {
                    return passwordResult;
                }
            }
            boolean deactivating = Boolean.TRUE.equals(persisted.getActive())
                    && !Boolean.TRUE.equals(systemUser.getActive());
            if (deactivating && !canDeactivateSms(tenant.getId())) {
                return ResultadoOperacao.falha("Não é possível desativar o último SMS ativo do tenant.");
            }

            persisted.setName(name);
            persisted.setEmail(email);
            persisted.setActive(Boolean.TRUE.equals(systemUser.getActive()));
            if (passwordChanged) {
                persisted.setPassword(passwordEncoder.encode(systemUser.getPassword()));
            }
            persisted.setUpdateDate(LocalDateTime.now());
            persisted.setUpdateUser(loggedUser.getLoginUsername());
            systemUserRepository.save(persisted);
            if (!Boolean.TRUE.equals(persisted.getActive()) || passwordChanged) {
                tenantSessionService.expireTenantUserSessions(tenant.getId(), persisted.getId());
            }
            return ResultadoOperacao.sucessoSemValor();
        }));
    }

    public ResultadoOperacao<Void> activateSmsUser(Long tenantId, Long userId, SystemUserDetails loggedUser) {
        return updateSmsUserActive(tenantId, userId, true, loggedUser);
    }

    public ResultadoOperacao<Void> deactivateSmsUser(Long tenantId, Long userId, SystemUserDetails loggedUser) {
        return updateSmsUserActive(tenantId, userId, false, loggedUser);
    }

    private ResultadoOperacao<Void> updateSmsUserActive(Long tenantId,
            Long userId,
            boolean active,
            SystemUserDetails loggedUser) {
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            return ResultadoOperacao.falha("Tenant não encontrado.");
        }
        return withTenantContext(tenant, () -> transactionTemplate.execute(status -> {
            SystemUser user = systemUserRepository.findById(userId)
                    .orElse(null);
            if (user == null) {
                return ResultadoOperacao.falha("Usuário SMS não encontrado.");
            }
            var roleResult = validateSmsRole(user);
            if (roleResult.falhou()) {
                return roleResult;
            }
            if (!active
                    && Boolean.TRUE.equals(user.getActive())
                    && !canDeactivateSms(tenant.getId())) {
                return ResultadoOperacao.falha("Não é possível desativar o último SMS ativo do tenant.");
            }
            user.setActive(active);
            user.setUpdateDate(LocalDateTime.now());
            user.setUpdateUser(loggedUser.getLoginUsername());
            systemUserRepository.save(user);
            if (!active) {
                tenantSessionService.expireTenantUserSessions(tenant.getId(), user.getId());
            }
            return ResultadoOperacao.sucessoSemValor();
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

    private ResultadoOperacao<SystemRole> findSmsRole() {
        return systemRoleRepository.findByRole(Roles.ROLE_SMS.toString())
                .map(ResultadoOperacao::sucesso)
                .orElseGet(() -> ResultadoOperacao.falha("Perfil SMS não cadastrado."));
    }

    private ResultadoOperacao<Void> validateSmsRole(SystemUser user) {
        boolean sms = user.getRoles().stream()
                .anyMatch(role -> Objects.equals(Roles.ROLE_SMS.toString(), role.getRole()));
        if (!sms) {
            return ResultadoOperacao.falha("Usuário não possui perfil SMS.");
        }
        return ResultadoOperacao.sucessoSemValor();
    }

    private ResultadoOperacao<Void> validatePassword(String password, String confirmation, boolean required) {
        if (passwordPolicyService != null) {
            return passwordPolicyService.validate(password, confirmation, required);
        }
        if (!required && !hasText(password)) {
            return ResultadoOperacao.sucessoSemValor();
        }
        if (!hasText(password) || !Objects.equals(password, confirmation)) {
            return ResultadoOperacao.falha("As senhas não conferem.");
        }
        return ResultadoOperacao.sucessoSemValor();
    }

    private String requireText(String value, String message) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeBlank(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean canDeactivateSms(Long tenantId) {
        tenantRepository.findByIdForUpdate(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant não encontrado."));
        return systemUserRepository.countActiveByRole(Roles.ROLE_SMS.toString()) > 1;
    }

    private SystemUser toSystemUser(AdminAccountCommandDTO command) {
        SystemUser user = new SystemUser();
        user.setId(command.getId());
        user.setUsername(command.getUsername());
        user.setPassword(command.getPassword());
        user.setConfirmPassword(command.getConfirmPassword());
        user.setName(command.getName());
        user.setEmail(command.getEmail());
        user.setActive(command.getActive());
        return user;
    }
}
