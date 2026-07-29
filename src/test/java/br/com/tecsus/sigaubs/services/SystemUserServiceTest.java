package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.dtos.SystemUserCommandDTO;
import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.SystemAdmin;
import br.com.tecsus.sigaubs.entities.SystemRole;
import br.com.tecsus.sigaubs.entities.SystemUser;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.repositories.BasicHealthUnitRepository;
import br.com.tecsus.sigaubs.repositories.SystemAdminRepository;
import br.com.tecsus.sigaubs.repositories.SystemRoleRepository;
import br.com.tecsus.sigaubs.repositories.SystemUserRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.tenancy.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.User;
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
import static org.mockito.ArgumentMatchers.anyString;
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

    @Mock
    private BasicHealthUnitRepository basicHealthUnitRepository;

    @Mock
    private PasswordPolicyService passwordPolicyService;

    @Mock
    private TenantSessionService tenantSessionService;

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
    void deveDelegarAtualizacaoDeUbsEConsultaDePapeis() throws Exception {
        TenantContextHolder.setTenant(1L, "afogados");
        SystemUser user = systemUser(1L, "user", role(1L, Roles.ROLE_USER));

        service.updateBasicHealthUnitSystemUsers(List.of(user));
        service.getRolesNotAdminAndNotManagement();

        verify(systemUserRepository).saveAll(List.of(user));
        verify(systemRoleRepository).findByRoleNotIn(any());
    }

    @Test
    void deveCadastrarUsuarioSomenteComGestorPerfilEUbsPermitidos() {
        SystemUserCommandDTO command = command(null);
        SystemRole allowedRole = role(1L, Roles.ROLE_ATENDENTE);
        BasicHealthUnit basicHealthUnit = br.com.tecsus.sigaubs.support.TestDataFactory.ubs(7L, "Centro");
        when(passwordPolicyService.validate("Forte#2026", "Forte#2026", true))
                .thenReturn(ResultadoOperacao.sucessoSemValor());
        when(systemRoleRepository.findById(1L)).thenReturn(Optional.of(allowedRole));
        when(basicHealthUnitRepository.findById(7L)).thenReturn(Optional.of(basicHealthUnit));
        when(passwordEncoder.encode("Forte#2026")).thenReturn("hash");
        SystemUserDetails manager =
                userDetails("sms", "SMS", null, 1L, "afogados", Roles.ROLE_SMS);

        var result = service.registerSystemUser(command, manager);

        assertThat(result.sucesso()).isTrue();
        ArgumentCaptor<SystemUser> captor = ArgumentCaptor.forClass(SystemUser.class);
        verify(systemUserRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("novo");
        assertThat(captor.getValue().getBasicHealthUnit()).isEqualTo(basicHealthUnit);
        assertThat(captor.getValue().getRoles()).containsExactly(allowedRole);

        assertThat(service.registerSystemUser(command, null).falhou()).isTrue();
        assertThat(service.registerSystemUser(
                command, userDetails("user", "User", 7L, 1L, "afogados", Roles.ROLE_USER))
                .falhou()).isTrue();
    }

    @Test
    void deveRecusarCadastroComSenhaPerfilOuUbsInvalidos() {
        SystemUserCommandDTO command = command(null);
        SystemUserDetails manager =
                userDetails("sms", "SMS", null, 1L, "afogados", Roles.ROLE_SMS);
        when(passwordPolicyService.validate(anyString(), anyString(), org.mockito.ArgumentMatchers.eq(true)))
                .thenReturn(ResultadoOperacao.falha("Senha inválida."));

        assertThat(service.registerSystemUser(command, manager).mensagem()).contains("Senha");

        when(passwordPolicyService.validate(anyString(), anyString(), org.mockito.ArgumentMatchers.eq(true)))
                .thenReturn(ResultadoOperacao.sucessoSemValor());
        assertThat(service.registerSystemUser(command, manager).mensagem()).contains("Perfil");

        SystemRole adminRole = role(99L, Roles.ROLE_ADMIN);
        when(systemRoleRepository.findById(1L)).thenReturn(Optional.of(adminRole));
        assertThat(service.registerSystemUser(command, manager).mensagem()).contains("Perfil");

        SystemRole allowedRole = role(1L, Roles.ROLE_USER);
        when(systemRoleRepository.findById(1L)).thenReturn(Optional.of(allowedRole));
        command.setBasicHealthUnit(null);
        assertThat(service.registerSystemUser(command, manager).mensagem()).contains("UBS");

        command.setBasicHealthUnit(7L);
        when(basicHealthUnitRepository.findById(7L)).thenReturn(Optional.empty());
        assertThat(service.registerSystemUser(command, manager).mensagem()).contains("UBS");
    }

    @Test
    void deveAtualizarUsuarioPersistidoERevogarSessoesEmEventoDeRisco() {
        TenantContextHolder.setTenant(1L, "afogados");
        SystemUserCommandDTO command = command(10L);
        SystemRole oldRole = role(1L, Roles.ROLE_USER);
        SystemRole newRole = role(2L, Roles.ROLE_ATENDENTE);
        command.setSelectedRoleId(2L);
        SystemUser persisted = systemUser(10L, "persisted", oldRole);
        persisted.setBasicHealthUnit(br.com.tecsus.sigaubs.support.TestDataFactory.ubs(6L, "Antiga"));
        BasicHealthUnit newUbs = br.com.tecsus.sigaubs.support.TestDataFactory.ubs(7L, "Nova");
        when(systemUserRepository.findById(10L)).thenReturn(Optional.of(persisted));
        when(systemRoleRepository.findById(2L)).thenReturn(Optional.of(newRole));
        when(basicHealthUnitRepository.findById(7L)).thenReturn(Optional.of(newUbs));
        when(passwordPolicyService.validate("Forte#2026", "Forte#2026", false))
                .thenReturn(ResultadoOperacao.sucessoSemValor());
        when(passwordEncoder.encode("Forte#2026")).thenReturn("hash-novo");
        SystemUserDetails manager =
                userDetails("sms", "SMS", null, 1L, "afogados", Roles.ROLE_SMS);

        var result = service.updateSystemUser(command, manager);

        assertThat(result.sucesso()).isTrue();
        assertThat(persisted.getPassword()).isEqualTo("hash-novo");
        assertThat(persisted.getBasicHealthUnit()).isEqualTo(newUbs);
        assertThat(persisted.getRoles()).containsExactly(newRole);
        verify(tenantSessionService).expireTenantUserSessions(1L, 10L);
    }

    @Test
    void deveRecusarAtualizacaoForaDaHierarquiaOuDoProprioPapel() {
        SystemUserCommandDTO command = command(null);
        SystemUserDetails manager = new SystemUserDetails(
                10L, "sms", "hash",
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        Roles.ROLE_SMS.toString())),
                "SMS", "sms@example.com", true, null, 1L, "afogados");
        assertThat(service.updateSystemUser(command, manager).mensagem()).contains("informado");

        command.setId(20L);
        when(systemUserRepository.findById(20L)).thenReturn(Optional.empty());
        assertThat(service.updateSystemUser(command, manager).mensagem()).contains("hierarquia");

        SystemUser sms = systemUser(20L, "outro-sms", role(5L, Roles.ROLE_SMS));
        when(systemUserRepository.findById(20L)).thenReturn(Optional.of(sms));
        assertThat(service.updateSystemUser(command, manager).mensagem()).contains("hierarquia");

        SystemUser self = systemUser(10L, "sms", role(1L, Roles.ROLE_USER));
        command.setId(10L);
        when(systemUserRepository.findById(10L)).thenReturn(Optional.of(self));
        assertThat(service.updateSystemUser(command, manager).mensagem()).contains("próprio papel");
    }

    @Test
    void deveRecusarCamposDeSegurancaInvalidosNaAtualizacao() {
        SystemUserCommandDTO command = command(20L);
        SystemUser persisted = systemUser(20L, "user", role(1L, Roles.ROLE_USER));
        when(systemUserRepository.findById(20L)).thenReturn(Optional.of(persisted));
        SystemUserDetails manager =
                userDetails("sms", "SMS", null, 1L, "afogados", Roles.ROLE_SMS);

        assertThat(service.updateSystemUser(command, manager).mensagem()).contains("Perfil");

        when(systemRoleRepository.findById(1L))
                .thenReturn(Optional.of(role(99L, Roles.ROLE_ADMIN)));
        assertThat(service.updateSystemUser(command, manager).mensagem()).contains("Perfil");

        when(systemRoleRepository.findById(1L))
                .thenReturn(Optional.of(role(1L, Roles.ROLE_USER)));
        command.setBasicHealthUnit(null);
        assertThat(service.updateSystemUser(command, manager).mensagem()).contains("UBS");

        command.setBasicHealthUnit(7L);
        when(basicHealthUnitRepository.findById(7L)).thenReturn(Optional.empty());
        assertThat(service.updateSystemUser(command, manager).mensagem()).contains("UBS");

        when(basicHealthUnitRepository.findById(7L))
                .thenReturn(Optional.of(br.com.tecsus.sigaubs.support.TestDataFactory.ubs(7L, "Centro")));
        when(passwordPolicyService.validate("Forte#2026", "Forte#2026", false))
                .thenReturn(ResultadoOperacao.falha("Senha inválida."));
        assertThat(service.updateSystemUser(command, manager).mensagem()).contains("Senha");
    }

    @Test
    void deveExcluirEConsultarSomenteUsuarioGerenciavel() {
        TenantContextHolder.setTenant(1L, "afogados");
        SystemUserDetails manager =
                userDetails("sms", "SMS", null, 1L, "afogados", Roles.ROLE_SMS);
        SystemUser user = systemUser(20L, "user", role(1L, Roles.ROLE_USER));
        when(systemUserRepository.findById(20L)).thenReturn(Optional.of(user));

        assertThat(service.findManageableSystemUserById(20L, manager)).isEqualTo(user);
        assertThat(service.deleteSystemUser(20L, manager).sucesso()).isTrue();
        verify(systemUserRepository).delete(user);
        verify(tenantSessionService).expireTenantUserSessions(1L, 20L);

        assertThat(service.findManageableSystemUserById(20L, null)).isNull();
        assertThat(service.deleteSystemUser(30L, manager).falhou()).isTrue();
        assertThat(service.deleteSystemUser(
                20L, userDetails("user", "User", 7L, 1L, "afogados", Roles.ROLE_USER))
                .falhou()).isTrue();
    }

    @Test
    void deveAtualizarHashDeAdminEUsuarioComIdentidadeEstavel() {
        TenantContextHolder.setTenant(1L, "afogados");
        SystemAdmin admin = systemAdmin("admin", "old");
        admin.setId(30L);
        SystemUserDetails adminDetails = new SystemUserDetails(
                30L, "admin", "old",
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        Roles.ROLE_ADMIN.toString())),
                "Admin", "admin@example.com", true, null, 1L, "afogados");
        when(systemAdminRepository.findById(30L)).thenReturn(Optional.of(admin));
        when(systemAdminRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThat(service.updatePassword(adminDetails, "new-admin")).isNotNull();
        assertThat(admin.getPassword()).isEqualTo("new-admin");

        SystemUser user = systemUser(20L, "user", role(1L, Roles.ROLE_USER));
        SystemUserDetails details = new SystemUserDetails(
                20L, "user", "old",
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                        Roles.ROLE_USER.toString())),
                "User", "user@example.com", true, 7L, 1L, "afogados");
        when(systemUserRepository.findById(20L)).thenReturn(Optional.of(user));
        when(systemAdminRepository.findByUsername("user")).thenReturn(Optional.empty());
        when(systemUserRepository.findByUsername("user")).thenReturn(Optional.of(user));

        assertThat(service.updatePassword(details, "new-user")).isNotNull();
        assertThat(user.getPassword()).isEqualTo("new-user");

        assertThatThrownBy(() -> service.updatePassword(
                new User("generic", "hash", List.of()), "new"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void deveRevogarSessoesAoAlterarVinculosDeUbs() {
        TenantContextHolder.setTenant(1L, "afogados");
        SystemUser one = systemUser(10L, "one", role(1L, Roles.ROLE_USER));
        SystemUser two = systemUser(20L, "two", role(1L, Roles.ROLE_USER));

        service.updateBasicHealthUnitSystemUsers(List.of(one, two));

        verify(tenantSessionService).expireTenantUserSessions(1L, 10L);
        verify(tenantSessionService).expireTenantUserSessions(1L, 20L);
    }

    private SystemUserCommandDTO command(Long id) {
        SystemUserCommandDTO command = new SystemUserCommandDTO();
        command.setId(id);
        command.setUsername(" Novo ");
        command.setPassword("Forte#2026");
        command.setConfirmPassword("Forte#2026");
        command.setName(" Novo Usuário ");
        command.setEmail(" NOVO@EXAMPLE.COM ");
        command.setActive(true);
        command.setBasicHealthUnit(7L);
        command.setSelectedRoleId(1L);
        return command;
    }
}
