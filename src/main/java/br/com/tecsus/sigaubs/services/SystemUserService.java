package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.UBSsystemUserDTO;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.entities.SystemRole;
import br.com.tecsus.sigaubs.entities.SystemAdmin;
import br.com.tecsus.sigaubs.entities.SystemUser;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.repositories.SystemRoleRepository;
import br.com.tecsus.sigaubs.repositories.SystemAdminRepository;
import br.com.tecsus.sigaubs.repositories.SystemUserRepository;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SystemUserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(SystemUserService.class);

    private final SystemUserRepository systemUserRepository;
    private final SystemRoleRepository systemRoleRepository;
    private final SystemAdminRepository systemAdminRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public SystemUserService(SystemUserRepository systemUserRepository,
            SystemRoleRepository systemRoleRepository,
            SystemAdminRepository systemAdminRepository,
            PasswordEncoder passwordEncoder) {
        this.systemUserRepository = systemUserRepository;
        this.systemRoleRepository = systemRoleRepository;
        this.systemAdminRepository = systemAdminRepository;
        this.passwordEncoder = passwordEncoder;
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

    @Transactional
    public ResultadoOperacao<Void> registerNotAdminSystemUser(SystemUser systemUser, SystemUserDetails loggedUser) {

        if (!systemUser.getPassword().equals(systemUser.getConfirmPassword())) {
            return ResultadoOperacao.falha("As senhas não conferem.");
        }

        SystemRole role = systemRoleRepository.findById(systemUser.getSelectedRoleId())
                .orElse(null);
        if (role == null) {
            log.error("[insert user] Erro ao encontrar role [id = {}]", systemUser.getSelectedRoleId());
            return ResultadoOperacao.falha("Erro ao cadastrar usuário.");
        }

        systemUser.setPassword(passwordEncoder.encode(systemUser.getPassword()));
        systemUser.setRoles(Set.of(role));
        systemUser.setCreationDate(LocalDateTime.now());
        systemUser.setCreationUser(loggedUser.getUsername());
        systemUser.setActive(true);

        systemUserRepository.save(systemUser);
        return ResultadoOperacao.sucessoSemValor();

    }

    public ResultadoOperacao<Void> updateNotAdminSystemUser(SystemUser systemUser) {

        SystemRole role = systemRoleRepository.findById(systemUser.getSelectedRoleId())
                .orElse(null);
        if (role == null) {
            log.error("[update user] Erro ao encontrar role [id = {}]", systemUser.getSelectedRoleId());
            return ResultadoOperacao.falha("Erro ao cadastrar usuário.");
        }

        systemUser.setPassword(passwordEncoder.encode(systemUser.getPassword()));
        systemUser.setUpdateUser(SecurityContextHolder.getContext().getAuthentication().getName());
        systemUser.setRoles(Set.of(role));
        systemUser.setUpdateDate(LocalDateTime.now());
        systemUser.setActive(systemUser.getActive());

        systemUserRepository.save(systemUser);
        return ResultadoOperacao.sucessoSemValor();
    }

    public List<SystemRole> getRolesNotAdmin() {
        return systemRoleRepository.findByRoleNot(Roles.ROLE_ADMIN.toString());
    }

    public List<SystemRole> getRolesNotAdminAndNotManagement() {
        return systemRoleRepository.findByRoleNotIn(List.of(Roles.ROLE_ADMIN.toString(), Roles.ROLE_SMS.toString()));
    }

    public List<SystemUser> findAllUsersByCreationUser() {
        return systemUserRepository.findAllByCreationUser(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    public Page<SystemUser> findAllUsersByCreationUserPaginated(SystemUser systemUser, PageRequest pageRequest) {
        return systemUserRepository.findSystemUsersPaginated(systemUser, pageRequest);
    }

    @Transactional(readOnly = true)
    public SystemUser findSystemUserById(Long id) {
        return systemUserRepository.findById(id).orElse(null);
    }

    @Transactional
    public ResultadoOperacao<Void> deleteNotAdminSystemUser(Long id) {
        SystemUser systemUser = systemUserRepository.findById(id).orElse(null);
        if (systemUser == null) {
            log.error("Usuário [id = {}] não encontrado.", id);
            return ResultadoOperacao.falha("Erro ao deletar usuário.");
        }
        systemUserRepository.delete(systemUser);
        return ResultadoOperacao.sucessoSemValor();
    }

    public List<UBSsystemUserDTO> findSystemUserByNameContaining(String username) {

        return systemUserRepository.findSystemUsersNameByNameContains(username);
    }

    public void updateBasicHealthUnitSystemUsers(List<SystemUser> systemUsers) {
        systemUserRepository.saveAll(systemUsers);
    }

    @Transactional(readOnly = true)
    public boolean validateSystemUserByPassword(String password, SystemUserDetails loggedUser) {
        if (loggedUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Roles.ROLE_ADMIN.toString()))) {
            return systemAdminRepository.findByUsername(loggedUser.getUsername())
                    .map(admin -> passwordEncoder.matches(password, admin.getPassword()))
                    .orElse(false);
        }

        return systemUserRepository.findByUsername(loggedUser.getUsername())
                .map(su -> passwordEncoder.matches(password, su.getPassword()))
                .orElse(false);
    }
}
