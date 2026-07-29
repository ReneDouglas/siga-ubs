package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.entities.SystemAdmin;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.repositories.SystemAdminRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AdminUserDetailsService implements UserDetailsService, UserDetailsPasswordService {

    private final SystemAdminRepository systemAdminRepository;

    public AdminUserDetailsService(SystemAdminRepository systemAdminRepository) {
        this.systemAdminRepository = systemAdminRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SystemAdmin admin = systemAdminRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Administrador não cadastrado."));

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
                null,
                null);
    }

    @Override
    @Transactional
    public UserDetails updatePassword(UserDetails user, String newPassword) {
        if (!(user instanceof SystemUserDetails details) || details.getUserId() == null) {
            throw new UsernameNotFoundException("Administrador não identificado.");
        }
        SystemAdmin admin = systemAdminRepository.findById(details.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("Administrador não cadastrado."));
        admin.setPassword(newPassword);
        systemAdminRepository.save(admin);
        return loadUserByUsername(details.getLoginUsername());
    }
}
