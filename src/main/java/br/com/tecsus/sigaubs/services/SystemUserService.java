package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.UBSsystemUserDTO;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.dtos.SystemUserCommandDTO;
import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.SystemRole;
import br.com.tecsus.sigaubs.entities.SystemAdmin;
import br.com.tecsus.sigaubs.entities.SystemUser;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.repositories.SystemRoleRepository;
import br.com.tecsus.sigaubs.repositories.SystemAdminRepository;
import br.com.tecsus.sigaubs.repositories.SystemUserRepository;
import br.com.tecsus.sigaubs.repositories.BasicHealthUnitRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.tenancy.TenantContext;
import br.com.tecsus.sigaubs.tenancy.TenantContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SystemUserService implements UserDetailsService, UserDetailsPasswordService {

    private static final Logger log = LoggerFactory.getLogger(SystemUserService.class);

    private final SystemUserRepository systemUserRepository;
    private final SystemRoleRepository systemRoleRepository;
    private final SystemAdminRepository systemAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final BasicHealthUnitRepository basicHealthUnitRepository;
    private final PasswordPolicyService passwordPolicyService;
    private final TenantSessionService tenantSessionService;

    private static final Set<String> ASSIGNABLE_TENANT_ROLES = Set.of(
            Roles.ROLE_USER.toString(),
            Roles.ROLE_ATENDENTE.toString(),
            Roles.ROLE_ENFERMEIRO.toString(),
            Roles.ROLE_ACS.toString());

    @Autowired
    public SystemUserService(SystemUserRepository systemUserRepository,
            SystemRoleRepository systemRoleRepository,
            SystemAdminRepository systemAdminRepository,
            PasswordEncoder passwordEncoder,
            BasicHealthUnitRepository basicHealthUnitRepository,
            PasswordPolicyService passwordPolicyService,
            TenantSessionService tenantSessionService) {
        this.systemUserRepository = systemUserRepository;
        this.systemRoleRepository = systemRoleRepository;
        this.systemAdminRepository = systemAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.basicHealthUnitRepository = basicHealthUnitRepository;
        this.passwordPolicyService = passwordPolicyService;
        this.tenantSessionService = tenantSessionService;
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, BadCredentialsException {

        TenantContext tenant = TenantContextHolder.getCurrent()
                .orElseThrow(() -> new UsernameNotFoundException("Tenant não informado."));

        var admin = systemAdminRepository.findByUsername(username);
        if (admin.isPresent()) {
            return buildAdminDetails(admin.get(), tenant);
        }

        SystemUser systemUser = systemUserRepository
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não cadastrado."));

        if (!Boolean.TRUE.equals(systemUser.getActive())) {
            throw new UsernameNotFoundException("Usuário inativo.");
        }

        return new SystemUserDetails(
                systemUser.getId(),
                systemUser.getUsername(),
                systemUser.getPassword(),
                systemUser.getRoles().stream().map(SystemRole::getRole).map(SimpleGrantedAuthority::new).collect(Collectors.toSet()),
                systemUser.getName(),
                systemUser.getEmail(),
                systemUser.getActive(),
                (systemUser.getBasicHealthUnit() != null) ? systemUser.getBasicHealthUnit().getId() : null,
                tenant.id(),
                tenant.slug()
                );

    }

    private UserDetails buildAdminDetails(SystemAdmin admin, TenantContext tenant) {
        if (!Boolean.TRUE.equals(admin.getActive())) {
            throw new UsernameNotFoundException("Administrador inativo.");
        }

        return new SystemUserDetails(
                admin.getId(),
                admin.getUsername(),
                admin.getPassword(),
                Set.of(new SimpleGrantedAuthority(Roles.ROLE_ADMIN.toString())),
                admin.getName(),
                admin.getEmail(),
                admin.getActive(),
                null,
                tenant.id(),
                tenant.slug());
    }

    public List<SystemRole> getRolesNotAdminAndNotManagement() {
        return systemRoleRepository.findByRoleNotIn(List.of(Roles.ROLE_ADMIN.toString(), Roles.ROLE_SMS.toString()));
    }

    public Page<SystemUser> findAllUsersByCreationUserPaginated(SystemUser systemUser, PageRequest pageRequest) {
        return systemUserRepository.findSystemUsersPaginated(systemUser, pageRequest);
    }

    @Transactional
    public ResultadoOperacao<Void> registerSystemUser(
            SystemUserCommandDTO command, SystemUserDetails loggedUser) {
        var actorResult = requireTenantUserManager(loggedUser);
        if (actorResult.falhou()) {
            return actorResult;
        }
        var passwordResult = passwordPolicyService.validate(
                command.getPassword(), command.getConfirmPassword(), true);
        if (passwordResult.falhou()) {
            return passwordResult;
        }
        var roleResult = loadAssignableRole(command.getSelectedRoleId());
        if (roleResult.falhou()) {
            return ResultadoOperacao.falha(roleResult.mensagem());
        }
        if (command.getBasicHealthUnit() == null) {
            return ResultadoOperacao.falha("UBS obrigatória.");
        }
        BasicHealthUnit basicHealthUnit = basicHealthUnitRepository.findById(command.getBasicHealthUnit())
                .orElse(null);
        if (basicHealthUnit == null) {
            return ResultadoOperacao.falha("UBS não encontrada.");
        }

        SystemUser systemUser = new SystemUser();
        systemUser.setUsername(command.getUsername().trim().toLowerCase());
        systemUser.setPassword(passwordEncoder.encode(command.getPassword()));
        systemUser.setName(command.getName().trim());
        systemUser.setEmail(command.getEmail().trim().toLowerCase());
        systemUser.setActive(true);
        systemUser.setBasicHealthUnit(basicHealthUnit);
        systemUser.setRoles(Set.of(roleResult.valor()));
        systemUser.setCreationDate(LocalDateTime.now());
        systemUser.setCreationUser(loggedUser.getLoginUsername());
        systemUserRepository.save(systemUser);
        return ResultadoOperacao.sucessoSemValor();
    }

    @Transactional
    public ResultadoOperacao<Void> updateSystemUser(
            SystemUserCommandDTO command, SystemUserDetails loggedUser) {
        var actorResult = requireTenantUserManager(loggedUser);
        if (actorResult.falhou()) {
            return actorResult;
        }
        if (command.getId() == null) {
            return ResultadoOperacao.falha("Usuário não informado.");
        }
        SystemUser persisted = systemUserRepository.findById(command.getId()).orElse(null);
        if (persisted == null || !hasOnlyAssignableRoles(persisted)) {
            return ResultadoOperacao.falha("Usuário não encontrado ou fora da hierarquia permitida.");
        }
        if (loggedUser.getUserId() != null && Objects.equals(persisted.getId(), loggedUser.getUserId())) {
            return ResultadoOperacao.falha("Não é possível alterar o próprio papel por este fluxo.");
        }

        var roleResult = loadAssignableRole(command.getSelectedRoleId());
        if (roleResult.falhou()) {
            return ResultadoOperacao.falha(roleResult.mensagem());
        }
        if (command.getBasicHealthUnit() == null) {
            return ResultadoOperacao.falha("UBS obrigatória.");
        }
        BasicHealthUnit basicHealthUnit = basicHealthUnitRepository.findById(command.getBasicHealthUnit())
                .orElse(null);
        if (basicHealthUnit == null) {
            return ResultadoOperacao.falha("UBS não encontrada.");
        }

        boolean passwordChanged = command.getPassword() != null && !command.getPassword().isBlank();
        if (passwordChanged) {
            var passwordResult = passwordPolicyService.validate(
                    command.getPassword(), command.getConfirmPassword(), false);
            if (passwordResult.falhou()) {
                return passwordResult;
            }
            persisted.setPassword(passwordEncoder.encode(command.getPassword()));
        }

        boolean securityStateChanged = passwordChanged
                || !persisted.getRoles().contains(roleResult.valor())
                || !Objects.equals(
                        persisted.getBasicHealthUnit() != null ? persisted.getBasicHealthUnit().getId() : null,
                        basicHealthUnit.getId())
                || !Objects.equals(persisted.getActive(), command.getActive());
        persisted.setName(command.getName().trim());
        persisted.setEmail(command.getEmail().trim().toLowerCase());
        persisted.setRoles(Set.of(roleResult.valor()));
        persisted.setBasicHealthUnit(basicHealthUnit);
        persisted.setActive(Boolean.TRUE.equals(command.getActive()));
        persisted.setUpdateUser(loggedUser.getLoginUsername());
        persisted.setUpdateDate(LocalDateTime.now());
        systemUserRepository.save(persisted);
        if (securityStateChanged && tenantSessionService != null) {
            tenantSessionService.expireTenantUserSessions(
                    TenantContextHolder.getRequiredTenantId(), persisted.getId());
        }
        return ResultadoOperacao.sucessoSemValor();
    }

    @Transactional
    public ResultadoOperacao<Void> deleteSystemUser(Long id, SystemUserDetails loggedUser) {
        var actorResult = requireTenantUserManager(loggedUser);
        if (actorResult.falhou()) {
            return actorResult;
        }
        SystemUser persisted = systemUserRepository.findById(id).orElse(null);
        if (persisted == null || !hasOnlyAssignableRoles(persisted)) {
            return ResultadoOperacao.falha("Usuário não encontrado ou fora da hierarquia permitida.");
        }
        systemUserRepository.delete(persisted);
        if (tenantSessionService != null) {
            tenantSessionService.expireTenantUserSessions(
                    TenantContextHolder.getRequiredTenantId(), persisted.getId());
        }
        return ResultadoOperacao.sucessoSemValor();
    }

    @Transactional(readOnly = true)
    public SystemUser findManageableSystemUserById(Long id, SystemUserDetails loggedUser) {
        if (requireTenantUserManager(loggedUser).falhou()) {
            return null;
        }
        return systemUserRepository.findById(id)
                .filter(this::hasOnlyAssignableRoles)
                .orElse(null);
    }

    private ResultadoOperacao<SystemRole> loadAssignableRole(Long roleId) {
        if (roleId == null) {
            return ResultadoOperacao.falha("Perfil obrigatório.");
        }
        SystemRole role = systemRoleRepository.findById(roleId).orElse(null);
        if (role == null || !ASSIGNABLE_TENANT_ROLES.contains(role.getRole())) {
            return ResultadoOperacao.falha("Perfil não permitido para este fluxo.");
        }
        return ResultadoOperacao.sucesso(role);
    }

    private boolean hasOnlyAssignableRoles(SystemUser user) {
        return user.getRoles() != null
                && !user.getRoles().isEmpty()
                && user.getRoles().stream().allMatch(role -> ASSIGNABLE_TENANT_ROLES.contains(role.getRole()));
    }

    private ResultadoOperacao<Void> requireTenantUserManager(SystemUserDetails loggedUser) {
        if (loggedUser == null) {
            return ResultadoOperacao.falha("Usuário autenticado não encontrado.");
        }
        boolean allowed = loggedUser.getAuthorities().stream()
                .anyMatch(authority -> Roles.ROLE_ADMIN.toString().equals(authority.getAuthority())
                        || Roles.ROLE_SMS.toString().equals(authority.getAuthority()));
        return allowed
                ? ResultadoOperacao.sucessoSemValor()
                : ResultadoOperacao.falha("Usuário sem permissão para administrar contas.");
    }

    public List<UBSsystemUserDTO> findSystemUserByNameContaining(String username) {

        return systemUserRepository.findSystemUsersNameByNameContains(username);
    }

    public void updateBasicHealthUnitSystemUsers(List<SystemUser> systemUsers) {
        systemUserRepository.saveAll(systemUsers);
        if (tenantSessionService != null) {
            Long tenantId = TenantContextHolder.getRequiredTenantId();
            systemUsers.forEach(user ->
                    tenantSessionService.expireTenantUserSessions(tenantId, user.getId()));
        }
    }

    @Transactional(readOnly = true)
    public boolean validateSystemUserByPassword(String password, SystemUserDetails loggedUser) {
        if (loggedUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Roles.ROLE_ADMIN.toString()))) {
            return systemAdminRepository.findByUsername(loggedUser.getLoginUsername())
                    .map(admin -> passwordEncoder.matches(password, admin.getPassword()))
                    .orElse(false);
        }

        return systemUserRepository.findByUsername(loggedUser.getLoginUsername())
                .map(su -> passwordEncoder.matches(password, su.getPassword()))
                .orElse(false);
    }

    @Override
    @Transactional
    public UserDetails updatePassword(UserDetails user, String newPassword) {
        if (!(user instanceof SystemUserDetails details) || details.getUserId() == null) {
            throw new UsernameNotFoundException("Usuário não identificado.");
        }
        boolean admin = details.getAuthorities().stream()
                .anyMatch(authority -> Roles.ROLE_ADMIN.toString().equals(authority.getAuthority()));
        if (admin) {
            SystemAdmin systemAdmin = systemAdminRepository.findById(details.getUserId())
                    .orElseThrow(() -> new UsernameNotFoundException("Administrador não cadastrado."));
            systemAdmin.setPassword(newPassword);
            systemAdminRepository.save(systemAdmin);
        } else {
            SystemUser systemUser = systemUserRepository.findById(details.getUserId())
                    .orElseThrow(() -> new UsernameNotFoundException("Usuário não cadastrado."));
            systemUser.setPassword(newPassword);
            systemUserRepository.save(systemUser);
        }
        return loadUserByUsername(details.getLoginUsername());
    }

}
