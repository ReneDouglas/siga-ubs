package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.entities.SystemAdmin;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.repositories.SystemAdminRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static br.com.tecsus.sigaubs.support.TestDataFactory.systemAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserDetailsServiceTest {

    private final SystemAdminRepository repository = mock(SystemAdminRepository.class);
    private final AdminUserDetailsService service = new AdminUserDetailsService(repository);

    @Test
    void deveCarregarSomenteAdministradorAtivo() {
        SystemAdmin admin = systemAdmin("admin", "hash");
        admin.setId(10L);
        when(repository.findByUsername("admin")).thenReturn(Optional.of(admin));

        SystemUserDetails result = (SystemUserDetails) service.loadUserByUsername("admin");

        assertThat(result.getUserId()).isEqualTo(10L);
        assertThat(result.getLoginUsername()).isEqualTo("admin");
        assertThat(result.getSessionPrincipalKey()).isEqualTo("admin:10");
        assertThat(result.getAuthorities()).extracting("authority")
                .containsExactly(Roles.ROLE_ADMIN.toString());

        admin.setActive(false);
        assertThatThrownBy(() -> service.loadUserByUsername("admin"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("inativo");
        when(repository.findByUsername("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void deveAtualizarHashERecarregarAdministrador() {
        SystemAdmin admin = systemAdmin("admin", "hash-antigo");
        admin.setId(10L);
        when(repository.findById(10L)).thenReturn(Optional.of(admin));
        when(repository.findByUsername("admin")).thenReturn(Optional.of(admin));
        SystemUserDetails details =
                (SystemUserDetails) service.loadUserByUsername("admin");

        SystemUserDetails result =
                (SystemUserDetails) service.updatePassword(details, "hash-novo");

        assertThat(admin.getPassword()).isEqualTo("hash-novo");
        assertThat(result.getPassword()).isEqualTo("hash-novo");
        verify(repository).save(admin);
    }

    @Test
    void deveRecusarAtualizacaoSemIdentidadePersistida() {
        User generic = new User("admin", "hash", List.of());
        assertThatThrownBy(() -> service.updatePassword(generic, "novo"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("identificado");

        SystemUserDetails unknown = new SystemUserDetails(
                99L, "admin", "hash", List.of(), "Admin", "admin@example.com",
                true, null, null, null);
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updatePassword(unknown, "novo"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("cadastrado");
    }
}
