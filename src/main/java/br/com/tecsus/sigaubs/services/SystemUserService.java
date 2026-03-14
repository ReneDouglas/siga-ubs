package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.UBSsystemUserDTO;
import br.com.tecsus.sigaubs.entities.SystemRole;
import br.com.tecsus.sigaubs.entities.SystemUser;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.repositories.SystemRoleRepository;
import br.com.tecsus.sigaubs.repositories.SystemUserRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.services.exceptions.InvalidConfirmPasswordException;
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
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public SystemUserService(SystemUserRepository systemUserRepository, SystemRoleRepository systemRoleRepository, PasswordEncoder passwordEncoder) {
        this.systemUserRepository = systemUserRepository;
        this.systemRoleRepository = systemRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, BadCredentialsException {

        SystemUser systemUser = systemUserRepository
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não cadastrado."));

        return new SystemUserDetails(
                systemUser.getUsername(),
                systemUser.getPassword(),
                systemUser.getRoles().stream().map(SystemRole::getRole).map(SimpleGrantedAuthority::new).collect(Collectors.toSet()),
                systemUser.getName(),
                systemUser.getEmail(),
                systemUser.getActive(),
                (systemUser.getBasicHealthUnit() != null) ? systemUser.getBasicHealthUnit().getId() : null
                );

    }

    @Transactional
    public void registerNotAdminSystemUser(SystemUser systemUser, SystemUserDetails loggedUser) throws Exception {

        if (!systemUser.getPassword().equals(systemUser.getConfirmPassword())) {
            throw new InvalidConfirmPasswordException("As senhas não conferem.");
        }

        SystemRole role = systemRoleRepository.findById(systemUser.getSelectedRoleId())
                .orElseThrow(() -> {
                    log.error("[insert user] Erro ao encontrar role [id = {}]", systemUser.getSelectedRoleId());
                    return new Exception("Erro ao cadastrar usuário.");
                });

        systemUser.setPassword(passwordEncoder.encode(systemUser.getPassword()));
        systemUser.setRoles(Set.of(role));
        systemUser.setCreationDate(LocalDateTime.now());
        systemUser.setCreationUser(loggedUser.getUsername());
        systemUser.setActive(true);

        systemUserRepository.save(systemUser);

    }

    public void updateNotAdminSystemUser(SystemUser systemUser) throws Exception {

        SystemRole role = systemRoleRepository.findById(systemUser.getSelectedRoleId())
                .orElseThrow(() -> {
                    log.error("[update user] Erro ao encontrar role [id = {}]", systemUser.getSelectedRoleId());
                    return new Exception("Erro ao cadastrar usuário.");
                });

        systemUser.setPassword(passwordEncoder.encode(systemUser.getPassword()));
        systemUser.setUpdateUser(SecurityContextHolder.getContext().getAuthentication().getName());
        systemUser.setRoles(Set.of(role));
        systemUser.setUpdateDate(LocalDateTime.now());
        systemUser.setActive(systemUser.getActive());

        systemUserRepository.save(systemUser);
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
    public void deleteNotAdminSystemUser(Long id) throws Exception{
        SystemUser systemUser = systemUserRepository.findById(id).orElseThrow(() -> {
            log.error("Usuário [id = {}] não encontrado.", id);
            return new Exception("Erro ao deletar usuário.");
        });
        systemUserRepository.delete(systemUser);
    }

    public List<UBSsystemUserDTO> findSystemUserByNameContaining(String username) {

        return systemUserRepository.findSystemUsersNameByNameContains(username);
    }

    public void updateBasicHealthUnitSystemUsers(List<SystemUser> systemUsers) {
        systemUserRepository.saveAll(systemUsers);
    }

    @Transactional(readOnly = true)
    public boolean validateSystemUserByPassword(String password, SystemUserDetails loggedUser) {
        SystemUser su = systemUserRepository.findByUsername(loggedUser.getUsername()).orElse(new SystemUser());
        return passwordEncoder.matches(password, su.getPassword());
    }
}
