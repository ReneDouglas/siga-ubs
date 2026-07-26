package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.entities.SystemAdmin;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.repositories.SystemAdminRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AdminUserDetailsService implements UserDetailsService {

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
}
