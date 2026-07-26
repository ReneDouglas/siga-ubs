package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.AdminUserSearchDTO;
import br.com.tecsus.sigaubs.entities.SystemAdmin;
import br.com.tecsus.sigaubs.repositories.SystemAdminRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

@Service
public class AdminUserManagementService {

    private final SystemAdminRepository systemAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantSessionService tenantSessionService;

    public AdminUserManagementService(SystemAdminRepository systemAdminRepository,
            PasswordEncoder passwordEncoder,
            TenantSessionService tenantSessionService) {
        this.systemAdminRepository = systemAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantSessionService = tenantSessionService;
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
    public SystemAdmin findById(Long id) {
        return systemAdminRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Administrador não encontrado."));
    }

    @Transactional
    public void create(SystemAdmin admin, SystemUserDetails loggedUser) {
        String username = requireText(admin.getUsername(), "Login obrigatório.").toLowerCase(Locale.ROOT);
        if (systemAdminRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Já existe um administrador com esse login.");
        }
        validatePassword(admin.getPassword(), admin.getConfirmPassword(), true);

        admin.setId(null);
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        admin.setName(requireText(admin.getName(), "Nome obrigatório."));
        admin.setEmail(requireText(admin.getEmail(), "E-mail obrigatório."));
        admin.setActive(true);
        admin.setCreationDate(LocalDateTime.now());
        admin.setCreationUser(loggedUser.getUsername());
        systemAdminRepository.save(admin);
    }

    @Transactional
    public void update(SystemAdmin admin, SystemUserDetails loggedUser) {
        SystemAdmin persisted = findById(admin.getId());
        boolean active = Boolean.TRUE.equals(admin.getActive());
        requireCanDeactivate(persisted, active, loggedUser);

        boolean passwordChanged = hasText(admin.getPassword());
        if (passwordChanged) {
            validatePassword(admin.getPassword(), admin.getConfirmPassword(), false);
            persisted.setPassword(passwordEncoder.encode(admin.getPassword()));
        }

        persisted.setName(requireText(admin.getName(), "Nome obrigatório."));
        persisted.setEmail(requireText(admin.getEmail(), "E-mail obrigatório."));
        persisted.setActive(active);
        persisted.setUpdateDate(LocalDateTime.now());
        persisted.setUpdateUser(loggedUser.getUsername());
        systemAdminRepository.save(persisted);

        if (!active || passwordChanged) {
            tenantSessionService.expireAdminUserSessions(persisted.getUsername());
        }
    }

    @Transactional
    public void activate(Long id, SystemUserDetails loggedUser) {
        updateActive(id, true, loggedUser);
    }

    @Transactional
    public void deactivate(Long id, SystemUserDetails loggedUser) {
        updateActive(id, false, loggedUser);
    }

    private void updateActive(Long id, boolean active, SystemUserDetails loggedUser) {
        SystemAdmin admin = findById(id);
        requireCanDeactivate(admin, active, loggedUser);
        admin.setActive(active);
        admin.setUpdateDate(LocalDateTime.now());
        admin.setUpdateUser(loggedUser.getUsername());
        systemAdminRepository.save(admin);
        if (!active) {
            tenantSessionService.expireAdminUserSessions(admin.getUsername());
        }
    }

    private void requireCanDeactivate(SystemAdmin admin, boolean active, SystemUserDetails loggedUser) {
        if (active) {
            return;
        }
        if (Objects.equals(admin.getUsername(), loggedUser.getUsername())) {
            throw new IllegalArgumentException("Não é possível desativar o próprio administrador logado.");
        }
        if (Boolean.TRUE.equals(admin.getActive()) && systemAdminRepository.countByActiveTrue() <= 1) {
            throw new IllegalArgumentException("Não é possível desativar o último administrador ativo.");
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
