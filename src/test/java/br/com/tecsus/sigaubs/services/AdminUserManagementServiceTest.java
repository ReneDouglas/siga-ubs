package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.AdminUserSearchDTO;
import br.com.tecsus.sigaubs.entities.SystemAdmin;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.repositories.SystemAdminRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static br.com.tecsus.sigaubs.support.TestDataFactory.systemAdmin;
import static br.com.tecsus.sigaubs.support.TestDataFactory.userDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserManagementServiceTest {

    @Mock
    private SystemAdminRepository systemAdminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TenantSessionService tenantSessionService;

    private AdminUserManagementService service;
    private SystemUserDetails loggedUser;

    @BeforeEach
    void setUp() {
        service = new AdminUserManagementService(systemAdminRepository, passwordEncoder, tenantSessionService);
        loggedUser = userDetails("root", "Root", null, 1L, "admin", Roles.ROLE_ADMIN);
    }

    @Test
    void deveCriarAdministradorNormalizandoDadosEAuditoria() {
        SystemAdmin admin = admin(" RootUser ", "Admin", "root@example.com", "123", "123", true);
        when(passwordEncoder.encode("123")).thenReturn("encoded");

        var resultado = service.create(admin, loggedUser);

        assertThat(resultado.sucesso()).isTrue();
        ArgumentCaptor<SystemAdmin> captor = ArgumentCaptor.forClass(SystemAdmin.class);
        verify(systemAdminRepository).save(captor.capture());
        SystemAdmin saved = captor.getValue();
        assertThat(saved.getId()).isNull();
        assertThat(saved.getUsername()).isEqualTo("rootuser");
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.getName()).isEqualTo("Admin");
        assertThat(saved.getEmail()).isEqualTo("root@example.com");
        assertThat(saved.getActive()).isTrue();
        assertThat(saved.getCreationUser()).isEqualTo("root");
        assertThat(saved.getCreationDate()).isNotNull();
    }

    @Test
    void deveBloquearCriacaoAdministradorInvalido() {
        assertThat(service.create(admin(" ", "Admin", "a@b.com", "123", "123", true), loggedUser).mensagem())
                .isEqualTo("Login obrigatório.");

        when(systemAdminRepository.existsByUsername("root")).thenReturn(true);
        assertThat(service.create(admin("root", "Admin", "a@b.com", "123", "123", true), loggedUser).mensagem())
                .isEqualTo("Já existe um administrador com esse login.");

        assertThat(service.create(admin("novo", "Admin", "a@b.com", "123", "321", true), loggedUser).mensagem())
                .isEqualTo("As senhas não conferem.");
        assertThat(service.create(admin("novo", " ", "a@b.com", "123", "123", true), loggedUser).mensagem())
                .isEqualTo("Nome obrigatório.");
        assertThat(service.create(admin("novo", "Admin", " ", "123", "123", true), loggedUser).mensagem())
                .isEqualTo("E-mail obrigatório.");
        verify(systemAdminRepository, never()).save(any());
    }

    @Test
    void deveAtualizarAdministradorComSenhaEExpirarSessoes() {
        SystemAdmin persisted = admin("admin", "Admin", "old@example.com", "old", null, true);
        persisted.setId(1L);
        SystemAdmin update = admin("ignorado", " Novo Nome ", " novo@example.com ", "nova", "nova", false);
        update.setId(1L);
        when(systemAdminRepository.findById(1L)).thenReturn(Optional.of(persisted));
        when(systemAdminRepository.findAllActiveForUpdate())
                .thenReturn(List.of(persisted, systemAdmin("other", "hash")));
        when(passwordEncoder.encode("nova")).thenReturn("encoded-new");

        var resultado = service.update(update, loggedUser);

        assertThat(resultado.sucesso()).isTrue();
        assertThat(persisted.getName()).isEqualTo("Novo Nome");
        assertThat(persisted.getEmail()).isEqualTo("novo@example.com");
        assertThat(persisted.getPassword()).isEqualTo("encoded-new");
        assertThat(persisted.getActive()).isFalse();
        assertThat(persisted.getUpdateUser()).isEqualTo("root");
        assertThat(persisted.getUpdateDate()).isNotNull();
        verify(systemAdminRepository).save(persisted);
        verify(tenantSessionService).expireAdminUserSessions(1L);
    }

    @Test
    void deveAtualizarAdministradorSemSenhaSemExpirarSessaoQuandoPermaneceAtivo() {
        SystemAdmin persisted = admin("admin", "Admin", "old@example.com", "old", null, true);
        persisted.setId(1L);
        SystemAdmin update = admin("admin", "Admin Atualizado", "novo@example.com", " ", " ", true);
        update.setId(1L);
        when(systemAdminRepository.findById(1L)).thenReturn(Optional.of(persisted));

        var resultado = service.update(update, loggedUser);

        assertThat(resultado.sucesso()).isTrue();
        assertThat(persisted.getPassword()).isEqualTo("old");
        assertThat(persisted.getActive()).isTrue();
        verify(passwordEncoder, never()).encode(any());
        verify(tenantSessionService, never()).expireAdminUserSessions(any());
    }

    @Test
    void deveValidarAtualizacaoAdministrador() {
        SystemAdmin update = admin("admin", "Admin", "admin@example.com", null, null, true);
        update.setId(99L);
        when(systemAdminRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.update(update, loggedUser).mensagem()).isEqualTo("Administrador não encontrado.");

        SystemAdmin persisted = admin("root", "Root", "root@example.com", "old", null, true);
        persisted.setId(1L);
        update.setId(1L);
        update.setActive(false);
        when(systemAdminRepository.findById(1L)).thenReturn(Optional.of(persisted));
        assertThat(service.update(update, loggedUser).mensagem())
                .isEqualTo("Não é possível desativar o próprio administrador logado.");

        persisted.setUsername("outro");
        when(systemAdminRepository.findAllActiveForUpdate()).thenReturn(List.of(persisted));
        assertThat(service.update(update, loggedUser).mensagem())
                .isEqualTo("Não é possível desativar o último administrador ativo.");

        update.setActive(true);
        update.setPassword("nova");
        update.setConfirmPassword("diferente");
        assertThat(service.update(update, loggedUser).mensagem()).isEqualTo("As senhas não conferem.");

        update.setPassword(null);
        update.setConfirmPassword(null);
        update.setName(" ");
        assertThat(service.update(update, loggedUser).mensagem()).isEqualTo("Nome obrigatório.");

        update.setName("Admin");
        update.setEmail(" ");
        assertThat(service.update(update, loggedUser).mensagem()).isEqualTo("E-mail obrigatório.");
        verify(systemAdminRepository, never()).save(any());
    }

    @Test
    void deveAtivarEDesativarAdministrador() {
        SystemAdmin admin = admin("admin", "Admin", "admin@example.com", "old", null, false);
        admin.setId(1L);
        when(systemAdminRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThat(service.activate(1L, loggedUser).sucesso()).isTrue();
        assertThat(admin.getActive()).isTrue();
        verify(systemAdminRepository).save(admin);

        admin.setActive(true);
        when(systemAdminRepository.findAllActiveForUpdate())
                .thenReturn(List.of(admin, systemAdmin("other", "hash")));
        assertThat(service.deactivate(1L, loggedUser).sucesso()).isTrue();
        assertThat(admin.getActive()).isFalse();
        verify(tenantSessionService).expireAdminUserSessions(1L);

        when(systemAdminRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.activate(99L, loggedUser).mensagem()).isEqualTo("Administrador não encontrado.");
    }

    @Test
    void deveBuscarEFiltrarAdministradores() {
        SystemAdmin admin = systemAdmin("admin", "123");
        when(systemAdminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(systemAdminRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.findById(1L).valor()).isSameAs(admin);
        assertThat(service.findById(99L).mensagem()).isEqualTo("Administrador não encontrado.");

        AdminUserSearchDTO search = new AdminUserSearchDTO();
        search.setUsername(" root ");
        search.setName(" Admin ");
        search.setActive(true);
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(admin));
        when(systemAdminRepository.findByFilters("root", "Admin", true, pageable)).thenReturn(page);

        assertThat(service.findAdmins(search, pageable)).isSameAs(page);
    }

    private SystemAdmin admin(String username, String name, String email, String password,
            String confirmation, Boolean active) {
        SystemAdmin admin = new SystemAdmin();
        admin.setUsername(username);
        admin.setName(name);
        admin.setEmail(email);
        admin.setPassword(password);
        admin.setConfirmPassword(confirmation);
        admin.setActive(active);
        return admin;
    }
}
