package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.entities.SystemAdmin;
import br.com.tecsus.sigaubs.entities.SystemRole;
import br.com.tecsus.sigaubs.entities.SystemUser;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.repositories.SystemAdminRepository;
import br.com.tecsus.sigaubs.repositories.SystemRoleRepository;
import br.com.tecsus.sigaubs.repositories.SystemUserRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.services.exceptions.InvalidConfirmPasswordException;
import br.com.tecsus.sigaubs.tenancy.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static br.com.tecsus.sigaubs.support.TestDataFactory.role;
import static br.com.tecsus.sigaubs.support.TestDataFactory.systemAdmin;
import static br.com.tecsus.sigaubs.support.TestDataFactory.systemUser;
import static br.com.tecsus.sigaubs.support.TestDataFactory.userDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemUserServiceTest {

    @Mock
    private SystemUserRepository systemUserRepository;

    @Mock
    private SystemRoleRepository systemRoleRepository;

    @Mock
    private SystemAdminRepository systemAdminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SystemUserService service;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveExigirTenantParaCarregarUsuario() {
        assertThatThrownBy(() -> service.loadUserByUsername("user"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Tenant");
    }

    @Test
    void deveCarregarAdminGlobalNoTenantAtual() {
        TenantContextHolder.setTenant(1L, "afogados");
        when(systemAdminRepository.findByUsername("admin")).thenReturn(Optional.of(systemAdmin("admin", "hash")));

        SystemUserDetails details = (SystemUserDetails) service.loadUserByUsername("admin");

        assertThat(details.getTenantId()).isEqualTo(1L);
        assertThat(details.getTenantSlug()).isEqualTo("afogados");
        assertThat(details.getAuthorities()).extracting("authority").containsExactly(Roles.ROLE_ADMIN.toString());
    }

    @Test
    void deveCarregarUsuarioTenantScopedAtivo() {
        TenantContextHolder.setTenant(2L, "caruaru");
        SystemRole role = role(1L, Roles.ROLE_ATENDENTE);
        SystemUser user = systemUser(1L, "user", role);
        when(systemAdminRepository.findByUsername("user")).thenReturn(Optional.empty());
        when(systemUserRepository.findByUsername("user")).thenReturn(Optional.of(user));

        SystemUserDetails details = (SystemUserDetails) service.loadUserByUsername("user");

        assertThat(details.getTenantId()).isEqualTo(2L);
        assertThat(details.getAuthorities()).extracting("authority").containsExactly(Roles.ROLE_ATENDENTE.toString());
    }

    @Test
    void deveBloquearUsuarioOuAdminInativo() {
        TenantContextHolder.setTenant(1L, "afogados");
        SystemAdmin admin = systemAdmin("admin", "hash");
        admin.setActive(false);
        when(systemAdminRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.loadUserByUsername("admin"))
                .isInstanceOf(UsernameNotFoundException.class);

        SystemUser user = systemUser(1L, "user", role(1L, Roles.ROLE_USER));
        user.setActive(false);
        when(systemAdminRepository.findByUsername("user")).thenReturn(Optional.empty());
        when(systemUserRepository.findByUsername("user")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.loadUserByUsername("user"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void deveCadastrarUsuarioNaoAdminComSenhaConfirmada() throws Exception {
        SystemRole role = role(1L, Roles.ROLE_ATENDENTE);
        SystemUser user = systemUser(1L, "user", role);
        user.setPassword("123456");
        user.setConfirmPassword("123456");
        user.setSelectedRoleId(1L);
        when(systemRoleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("123456")).thenReturn("hash");

        service.registerNotAdminSystemUser(user,
                userDetails("admin", "Admin", null, 1L, "afogados", Roles.ROLE_SMS));

        ArgumentCaptor<SystemUser> captor = ArgumentCaptor.forClass(SystemUser.class);
        verify(systemUserRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hash");
        assertThat(captor.getValue().getCreationUser()).isEqualTo("admin");
        assertThat(captor.getValue().getActive()).isTrue();
        assertThat(captor.getValue().getRoles()).containsExactly(role);
    }

    @Test
    void deveBloquearCadastroComConfirmacaoDiferente() {
        SystemUser user = systemUser(1L, "user", role(1L, Roles.ROLE_ATENDENTE));
        user.setPassword("123");
        user.setConfirmPassword("456");

        assertThatThrownBy(() -> service.registerNotAdminSystemUser(
                user,
                userDetails("admin", "Admin", null, 1L, "afogados", Roles.ROLE_SMS)))
                .isInstanceOf(InvalidConfirmPasswordException.class);
    }

    @Test
    void deveAtualizarUsuarioNaoAdminComUsuarioAutenticado() throws Exception {
        SystemRole role = role(1L, Roles.ROLE_USER);
        SystemUser user = systemUser(1L, "user", role);
        user.setSelectedRoleId(1L);
        user.setPassword("nova");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "senha"));
        when(systemRoleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("nova")).thenReturn("hash-nova");

        service.updateNotAdminSystemUser(user);

        assertThat(user.getPassword()).isEqualTo("hash-nova");
        assertThat(user.getUpdateUser()).isEqualTo("admin");
        assertThat(user.getUpdateDate()).isNotNull();
        verify(systemUserRepository).save(user);
    }

    @Test
    void deveValidarSenhaDeAdminEUsuario() {
        var adminDetails = userDetails("admin", "Admin", null, 1L, "afogados", Roles.ROLE_ADMIN);
        var userDetails = userDetails("user", "Usuário", 1L, 1L, "afogados", Roles.ROLE_USER);
        when(systemAdminRepository.findByUsername("admin")).thenReturn(Optional.of(systemAdmin("admin", "hash-admin")));
        when(systemUserRepository.findByUsername("user")).thenReturn(Optional.of(systemUser(1L, "user", role(1L, Roles.ROLE_USER))));
        when(passwordEncoder.matches("ok", "hash-admin")).thenReturn(true);
        when(passwordEncoder.matches("ok", "{noop}123456")).thenReturn(true);

        assertThat(service.validateSystemUserByPassword("ok", adminDetails)).isTrue();
        assertThat(service.validateSystemUserByPassword("ok", userDetails)).isTrue();
        assertThat(service.validateSystemUserByPassword("bad", userDetails)).isFalse();
    }

    @Test
    void deveDelegarConsultasEDelete() throws Exception {
        SystemUser user = systemUser(1L, "user", role(1L, Roles.ROLE_USER));
        when(systemUserRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(service.findSystemUserById(1L)).isEqualTo(user);
        service.deleteNotAdminSystemUser(1L);
        service.updateBasicHealthUnitSystemUsers(List.of(user));
        service.getRolesNotAdmin();
        service.getRolesNotAdminAndNotManagement();

        verify(systemUserRepository).delete(user);
        verify(systemUserRepository).saveAll(List.of(user));
        verify(systemRoleRepository).findByRoleNot(Roles.ROLE_ADMIN.toString());
        verify(systemRoleRepository).findByRoleNotIn(any());
    }
}
