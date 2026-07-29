package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.AdminUserSearchDTO;
import br.com.tecsus.sigaubs.dtos.AdminAccountCommandDTO;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.entities.SystemAdmin;
import br.com.tecsus.sigaubs.repositories.SystemAdminRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

@Service
public class AdminUserManagementService {

    private final SystemAdminRepository systemAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantSessionService tenantSessionService;
    private final PasswordPolicyService passwordPolicyService;

    @Autowired
    public AdminUserManagementService(SystemAdminRepository systemAdminRepository,
            PasswordEncoder passwordEncoder,
            TenantSessionService tenantSessionService,
            PasswordPolicyService passwordPolicyService) {
        this.systemAdminRepository = systemAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantSessionService = tenantSessionService;
        this.passwordPolicyService = passwordPolicyService;
    }

    AdminUserManagementService(SystemAdminRepository systemAdminRepository,
            PasswordEncoder passwordEncoder,
            TenantSessionService tenantSessionService) {
        this(systemAdminRepository, passwordEncoder, tenantSessionService, null);
    }

    @Transactional
    public ResultadoOperacao<Void> create(
            AdminAccountCommandDTO command, SystemUserDetails loggedUser) {
        return create(toSystemAdmin(command), loggedUser);
    }

    @Transactional
    public ResultadoOperacao<Void> update(
            AdminAccountCommandDTO command, SystemUserDetails loggedUser) {
        return update(toSystemAdmin(command), loggedUser);
    }

    @Transactional(readOnly = true)
    public Page<SystemAdmin> findAdmins(AdminUserSearchDTO search, Pageable pageable) {
        return systemAdminRepository.findByFilters(
                normalizeBlank(search != null ? search.getUsername() : null),
                normalizeBlank(search != null ? search.getName() : null),
                search != null ? search.getActive() : null,
                pageable);
    }

    @Transactional(readOnly = true)
    public ResultadoOperacao<SystemAdmin> findById(Long id) {
        return systemAdminRepository.findById(id)
                .map(ResultadoOperacao::sucesso)
                .orElseGet(() -> ResultadoOperacao.falha("Administrador não encontrado."));
    }

    @Transactional
    public ResultadoOperacao<Void> create(SystemAdmin admin, SystemUserDetails loggedUser) {
        String username = normalizeBlank(admin.getUsername());
        if (username == null) {
            return ResultadoOperacao.falha("Login obrigatório.");
        }
        username = username.toLowerCase(Locale.ROOT);
        if (systemAdminRepository.existsByUsername(username)) {
            return ResultadoOperacao.falha("Já existe um administrador com esse login.");
        }
        var passwordResult = validatePassword(admin.getPassword(), admin.getConfirmPassword(), true);
        if (passwordResult.falhou()) {
            return passwordResult;
        }
        String name = normalizeBlank(admin.getName());
        if (name == null) {
            return ResultadoOperacao.falha("Nome obrigatório.");
        }
        String email = normalizeBlank(admin.getEmail());
        if (email == null) {
            return ResultadoOperacao.falha("E-mail obrigatório.");
        }

        admin.setId(null);
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        admin.setName(name);
        admin.setEmail(email);
        admin.setActive(true);
        admin.setCreationDate(LocalDateTime.now());
        admin.setCreationUser(loggedUser.getLoginUsername());
        systemAdminRepository.save(admin);
        return ResultadoOperacao.sucessoSemValor();
    }

    @Transactional
    public ResultadoOperacao<Void> update(SystemAdmin admin, SystemUserDetails loggedUser) {
        SystemAdmin persisted = systemAdminRepository.findById(admin.getId()).orElse(null);
        if (persisted == null) {
            return ResultadoOperacao.falha("Administrador não encontrado.");
        }
        boolean active = Boolean.TRUE.equals(admin.getActive());
        var canDeactivate = validateCanDeactivate(persisted, active, loggedUser);
        if (canDeactivate.falhou()) {
            return canDeactivate;
        }

        boolean passwordChanged = hasText(admin.getPassword());
        if (passwordChanged) {
            var passwordResult = validatePassword(admin.getPassword(), admin.getConfirmPassword(), false);
            if (passwordResult.falhou()) {
                return passwordResult;
            }
        }

        String name = normalizeBlank(admin.getName());
        if (name == null) {
            return ResultadoOperacao.falha("Nome obrigatório.");
        }
        String email = normalizeBlank(admin.getEmail());
        if (email == null) {
            return ResultadoOperacao.falha("E-mail obrigatório.");
        }

        if (passwordChanged) {
            persisted.setPassword(passwordEncoder.encode(admin.getPassword()));
        }
        persisted.setName(name);
        persisted.setEmail(email);
        persisted.setActive(active);
        persisted.setUpdateDate(LocalDateTime.now());
        persisted.setUpdateUser(loggedUser.getLoginUsername());
        systemAdminRepository.save(persisted);

        if (!active || passwordChanged) {
            tenantSessionService.expireAdminUserSessions(persisted.getId());
        }
        return ResultadoOperacao.sucessoSemValor();
    }

    @Transactional
    public ResultadoOperacao<Void> activate(Long id, SystemUserDetails loggedUser) {
        return updateActive(id, true, loggedUser);
    }

    @Transactional
    public ResultadoOperacao<Void> deactivate(Long id, SystemUserDetails loggedUser) {
        return updateActive(id, false, loggedUser);
    }

    private ResultadoOperacao<Void> updateActive(Long id, boolean active, SystemUserDetails loggedUser) {
        SystemAdmin admin = systemAdminRepository.findById(id).orElse(null);
        if (admin == null) {
            return ResultadoOperacao.falha("Administrador não encontrado.");
        }
        var canDeactivate = validateCanDeactivate(admin, active, loggedUser);
        if (canDeactivate.falhou()) {
            return canDeactivate;
        }
        admin.setActive(active);
        admin.setUpdateDate(LocalDateTime.now());
        admin.setUpdateUser(loggedUser.getLoginUsername());
        systemAdminRepository.save(admin);
        if (!active) {
            tenantSessionService.expireAdminUserSessions(admin.getId());
        }
        return ResultadoOperacao.sucessoSemValor();
    }

    private ResultadoOperacao<Void> validateCanDeactivate(SystemAdmin admin, boolean active,
            SystemUserDetails loggedUser) {
        if (active) {
            return ResultadoOperacao.sucessoSemValor();
        }
        if (Objects.equals(admin.getUsername(), loggedUser.getLoginUsername())) {
            return ResultadoOperacao.falha("Não é possível desativar o próprio administrador logado.");
        }
        if (Boolean.TRUE.equals(admin.getActive())
                && systemAdminRepository.findAllActiveForUpdate().size() <= 1) {
            return ResultadoOperacao.falha("Não é possível desativar o último administrador ativo.");
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

    private String normalizeBlank(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private SystemAdmin toSystemAdmin(AdminAccountCommandDTO command) {
        SystemAdmin admin = new SystemAdmin();
        admin.setId(command.getId());
        admin.setUsername(command.getUsername());
        admin.setPassword(command.getPassword());
        admin.setConfirmPassword(command.getConfirmPassword());
        admin.setName(command.getName());
        admin.setEmail(command.getEmail());
        admin.setActive(command.getActive());
        return admin;
    }
}
