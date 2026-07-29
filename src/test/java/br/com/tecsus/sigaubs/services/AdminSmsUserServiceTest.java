package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.SmsUserSearchDTO;
import br.com.tecsus.sigaubs.entities.SystemRole;
import br.com.tecsus.sigaubs.entities.SystemUser;
import br.com.tecsus.sigaubs.entities.Tenant;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.repositories.SystemRoleRepository;
import br.com.tecsus.sigaubs.repositories.SystemUserRepository;
import br.com.tecsus.sigaubs.repositories.TenantRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.tenancy.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static br.com.tecsus.sigaubs.support.TestDataFactory.role;
import static br.com.tecsus.sigaubs.support.TestDataFactory.tenant;
import static br.com.tecsus.sigaubs.support.TestDataFactory.userDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSmsUserServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private SystemUserRepository systemUserRepository;

    @Mock
    private SystemRoleRepository systemRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private TenantSessionService tenantSessionService;

    private AdminSmsUserService service;
    private Tenant tenant;
    private SystemRole smsRole;
    private SystemUserDetails loggedUser;

    @BeforeEach
    void setUp() {
        service = new AdminSmsUserService(tenantRepository, systemUserRepository, systemRoleRepository,
                passwordEncoder, transactionTemplate, tenantSessionService);
        tenant = tenant(1L, "afogados");
        smsRole = role(2L, Roles.ROLE_SMS);
        loggedUser = userDetails("root", "Root", null, 1L, "admin", Roles.ROLE_ADMIN);
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(new SimpleTransactionStatus());
        });
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void deveCriarUsuarioSmsComTenantContextoAuditoriaERoleSms() {
        SystemUser user = smsUser(" sms ", " SMS User ", " sms@example.com ", "123", "123", true);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(systemRoleRepository.findByRole(Roles.ROLE_SMS.toString())).thenReturn(Optional.of(smsRole));
        when(passwordEncoder.encode("123")).thenReturn("encoded");

        var resultado = service.createSmsUser(1L, user, loggedUser);

        assertThat(resultado.sucesso()).isTrue();
        ArgumentCaptor<SystemUser> captor = ArgumentCaptor.forClass(SystemUser.class);
        verify(systemUserRepository).save(captor.capture());
        SystemUser saved = captor.getValue();
        assertThat(saved.getId()).isNull();
        assertThat(saved.getUsername()).isEqualTo("sms");
        assertThat(saved.getName()).isEqualTo("SMS User");
        assertThat(saved.getEmail()).isEqualTo("sms@example.com");
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.getActive()).isTrue();
        assertThat(saved.getBasicHealthUnit()).isNull();
        assertThat(saved.getRoles()).containsExactly(smsRole);
        assertThat(saved.getTenantId()).isEqualTo(1L);
        assertThat(saved.getCreationUser()).isEqualTo("root");
        assertThat(saved.getCreationDate()).isNotNull();
        assertThat(TenantContextHolder.getCurrentTenantId()).isEmpty();
    }

    @Test
    void deveBloquearCriacaoUsuarioSmsQuandoTenantNaoExiste() {
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

        var resultado = service.createSmsUser(99L, smsUser("sms", "SMS", "sms@example.com", "123", "123", true),
                loggedUser);

        assertThat(resultado.falhou()).isTrue();
        assertThat(resultado.mensagem()).isEqualTo("Tenant não encontrado.");
        verify(systemUserRepository, never()).save(any());
    }

    @Test
    void deveValidarDadosObrigatoriosAoCriarUsuarioSms() {
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(systemRoleRepository.findByRole(Roles.ROLE_SMS.toString())).thenReturn(Optional.of(smsRole));

        assertThat(service.createSmsUser(1L, smsUser("sms", "SMS", "sms@example.com", "123", "321", true),
                loggedUser).mensagem()).isEqualTo("As senhas não conferem.");

        when(systemRoleRepository.findByRole(Roles.ROLE_SMS.toString())).thenReturn(Optional.empty());
        assertThat(service.createSmsUser(1L, smsUser("sms", "SMS", "sms@example.com", "123", "123", true),
                loggedUser).mensagem()).isEqualTo("Perfil SMS não cadastrado.");

        when(systemRoleRepository.findByRole(Roles.ROLE_SMS.toString())).thenReturn(Optional.of(smsRole));
        assertThat(service.createSmsUser(1L, smsUser(" ", "SMS", "sms@example.com", "123", "123", true),
                loggedUser).mensagem()).isEqualTo("Login obrigatório.");
        assertThat(service.createSmsUser(1L, smsUser("sms", " ", "sms@example.com", "123", "123", true),
                loggedUser).mensagem()).isEqualTo("Nome obrigatório.");
        assertThat(service.createSmsUser(1L, smsUser("sms", "SMS", " ", "123", "123", true),
                loggedUser).mensagem()).isEqualTo("E-mail obrigatório.");
        verify(systemUserRepository, never()).save(any());
    }

    @Test
    void deveAtualizarUsuarioSmsComSenhaEInativacaoExpirandoSessao() {
        SystemUser persisted = smsUser("sms", "Antigo", "old@example.com", "old", null, true);
        persisted.setId(10L);
        persisted.setRoles(Set.of(smsRole));
        SystemUser update = smsUser("ignorado", " Novo ", " novo@example.com ", "nova", "nova", false);
        update.setId(10L);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tenant));
        when(systemUserRepository.findById(10L)).thenReturn(Optional.of(persisted));
        when(systemUserRepository.countActiveByRole(Roles.ROLE_SMS.toString())).thenReturn(2L);
        when(passwordEncoder.encode("nova")).thenReturn("encoded-new");

        var resultado = service.updateSmsUser(1L, update, loggedUser);

        assertThat(resultado.sucesso()).isTrue();
        assertThat(persisted.getName()).isEqualTo("Novo");
        assertThat(persisted.getEmail()).isEqualTo("novo@example.com");
        assertThat(persisted.getPassword()).isEqualTo("encoded-new");
        assertThat(persisted.getActive()).isFalse();
        assertThat(persisted.getUpdateUser()).isEqualTo("root");
        assertThat(persisted.getUpdateDate()).isNotNull();
        verify(systemUserRepository).save(persisted);
        verify(tenantSessionService).expireTenantUserSessions(1L, 10L);
        assertThat(TenantContextHolder.getCurrentTenantId()).isEmpty();
    }

    @Test
    void deveAtualizarUsuarioSmsSemSenhaMantendoSessaoQuandoAtivo() {
        SystemUser persisted = smsUser("sms", "Antigo", "old@example.com", "old", null, true);
        persisted.setId(10L);
        persisted.setRoles(Set.of(smsRole));
        SystemUser update = smsUser("sms", "Novo", "novo@example.com", " ", " ", true);
        update.setId(10L);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(systemUserRepository.findById(10L)).thenReturn(Optional.of(persisted));

        var resultado = service.updateSmsUser(1L, update, loggedUser);

        assertThat(resultado.sucesso()).isTrue();
        assertThat(persisted.getPassword()).isEqualTo("old");
        assertThat(persisted.getActive()).isTrue();
        verify(passwordEncoder, never()).encode(any());
        verify(tenantSessionService, never()).expireTenantUserSessions(any(), any());
    }

    @Test
    void deveValidarAtualizacaoUsuarioSms() {
        SystemUser update = smsUser("sms", "SMS", "sms@example.com", null, null, true);
        update.setId(10L);
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.updateSmsUser(99L, update, loggedUser).mensagem()).isEqualTo("Tenant não encontrado.");

        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(systemUserRepository.findById(10L)).thenReturn(Optional.empty());
        assertThat(service.updateSmsUser(1L, update, loggedUser).mensagem()).isEqualTo("Usuário SMS não encontrado.");

        SystemUser persisted = smsUser("user", "User", "user@example.com", "old", null, true);
        persisted.setId(10L);
        persisted.setRoles(Set.of(role(3L, Roles.ROLE_ATENDENTE)));
        when(systemUserRepository.findById(10L)).thenReturn(Optional.of(persisted));
        assertThat(service.updateSmsUser(1L, update, loggedUser).mensagem())
                .isEqualTo("Usuário não possui perfil SMS.");

        persisted.setRoles(Set.of(smsRole));
        update.setName(" ");
        assertThat(service.updateSmsUser(1L, update, loggedUser).mensagem()).isEqualTo("Nome obrigatório.");

        update.setName("SMS");
        update.setEmail(" ");
        assertThat(service.updateSmsUser(1L, update, loggedUser).mensagem()).isEqualTo("E-mail obrigatório.");

        update.setEmail("sms@example.com");
        update.setPassword("nova");
        update.setConfirmPassword("diferente");
        assertThat(service.updateSmsUser(1L, update, loggedUser).mensagem()).isEqualTo("As senhas não conferem.");
        verify(systemUserRepository, never()).save(any());
    }

    @Test
    void deveAtivarEDesativarUsuarioSms() {
        SystemUser user = smsUser("sms", "SMS", "sms@example.com", "old", null, false);
        user.setId(10L);
        user.setRoles(Set.of(smsRole));
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tenant));
        when(systemUserRepository.findById(10L)).thenReturn(Optional.of(user));
        when(systemUserRepository.countActiveByRole(Roles.ROLE_SMS.toString())).thenReturn(2L);

        assertThat(service.activateSmsUser(1L, 10L, loggedUser).sucesso()).isTrue();
        assertThat(user.getActive()).isTrue();
        verify(systemUserRepository).save(user);

        assertThat(service.deactivateSmsUser(1L, 10L, loggedUser).sucesso()).isTrue();
        assertThat(user.getActive()).isFalse();
        verify(tenantSessionService).expireTenantUserSessions(1L, 10L);

        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.activateSmsUser(99L, 10L, loggedUser).mensagem()).isEqualTo("Tenant não encontrado.");

        when(systemUserRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.activateSmsUser(1L, 99L, loggedUser).mensagem()).isEqualTo("Usuário SMS não encontrado.");
    }

    @Test
    void deveListarUsuariosSmsMantendoOrdenacaoDosIds() {
        SystemUser first = smsUser("first", "First", "first@example.com", null, null, true);
        first.setId(1L);
        SystemUser second = smsUser("second", "Second", "second@example.com", null, null, true);
        second.setId(2L);
        SmsUserSearchDTO search = new SmsUserSearchDTO();
        search.setUsername(" sms ");
        search.setName(" Maria ");
        search.setActive(true);
        var pageable = PageRequest.of(0, 10);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(systemUserRepository.findIdsByRoleAndFilters(
                eq(Roles.ROLE_SMS.toString()), eq("sms"), eq("Maria"), eq(true), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(2L, 1L), pageable, 2));
        when(systemUserRepository.findAllByIdIn(List.of(2L, 1L))).thenReturn(List.of(first, second));

        var page = service.findSmsUsers(1L, search, pageable);

        assertThat(page.getContent()).containsExactly(second, first);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void deveRetornarPaginaVaziaQuandoNaoHaUsuariosSms() {
        var pageable = PageRequest.of(0, 10);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(systemUserRepository.findIdsByRoleAndFilters(
                eq(Roles.ROLE_SMS.toString()), eq(null), eq(null), eq(null), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        var page = service.findSmsUsers(1L, null, pageable);

        assertThat(page.getContent()).isEmpty();
        verify(systemUserRepository, never()).findAllByIdIn(any());
    }

    @Test
    void deveBuscarUsuarioSmsValidandoPerfil() {
        SystemUser user = smsUser("sms", "SMS", "sms@example.com", null, null, true);
        user.setId(10L);
        user.setRoles(Set.of(smsRole));
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(systemUserRepository.findById(10L)).thenReturn(Optional.of(user));

        assertThat(service.findSmsUser(1L, 10L)).isSameAs(user);

        when(systemUserRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findSmsUser(1L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Usuário SMS não encontrado.");

        SystemUser nonSms = smsUser("user", "User", "user@example.com", null, null, true);
        nonSms.setRoles(Set.of(role(3L, Roles.ROLE_ATENDENTE)));
        when(systemUserRepository.findById(11L)).thenReturn(Optional.of(nonSms));
        assertThatThrownBy(() -> service.findSmsUser(1L, 11L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Usuário não possui perfil SMS.");
    }

    @Test
    void deveFalharAoBuscarTenantInexistente() {
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findTenant(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tenant não encontrado.");
    }

    private SystemUser smsUser(String username, String name, String email, String password,
            String confirmation, Boolean active) {
        SystemUser user = new SystemUser();
        user.setUsername(username);
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setConfirmPassword(confirmation);
        user.setActive(active);
        return user;
    }
}
