package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.entities.Tenant;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.enums.TenantStatus;
import br.com.tecsus.sigaubs.repositories.TenantRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static br.com.tecsus.sigaubs.support.TestDataFactory.tenant;
import static br.com.tecsus.sigaubs.support.TestDataFactory.userDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantManagementServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantSessionService tenantSessionService;

    private TenantManagementService service;
    private SystemUserDetails loggedUser;

    @BeforeEach
    void setUp() {
        service = new TenantManagementService(tenantRepository, tenantSessionService,
                "sigaubs.com.br:443", "admin");
        loggedUser = userDetails("root", "Root", null, 1L, "admin", Roles.ROLE_ADMIN);
    }

    @Test
    void deveCriarTenantNormalizandoSlugDominioEAuditoria() {
        Tenant tenant = new Tenant();
        tenant.setId(99L);
        tenant.setSlug(" Afogados ");
        tenant.setName(" Afogados da Ingazeira ");
        tenant.setDomain(" ");
        when(tenantRepository.save(tenant)).thenReturn(tenant);

        ResultadoOperacao<Tenant> resultado = service.create(tenant, loggedUser);

        assertThat(resultado.sucesso()).isTrue();
        assertThat(resultado.valor()).isSameAs(tenant);
        assertThat(tenant.getId()).isNull();
        assertThat(tenant.getSlug()).isEqualTo("afogados");
        assertThat(tenant.getName()).isEqualTo("Afogados da Ingazeira");
        assertThat(tenant.getDomain()).isEqualTo("afogados.sigaubs.com.br");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(tenant.getCreationUser()).isEqualTo("root");
        assertThat(tenant.getCreationDate()).isNotNull();
        verify(tenantRepository).save(tenant);
    }

    @Test
    void deveBloquearCriacaoTenantInvalidoOuDuplicado() {
        assertThat(service.create(tenantParaCriacao("slug invalido", "Tenant", null), loggedUser).mensagem())
                .isEqualTo("Slug inválido.");
        assertThat(service.create(tenantParaCriacao("admin", "Tenant", null), loggedUser).mensagem())
                .isEqualTo("Slug reservado para o admin global.");

        when(tenantRepository.existsBySlug("afogados")).thenReturn(true);
        assertThat(service.create(tenantParaCriacao("afogados", "Tenant", null), loggedUser).mensagem())
                .isEqualTo("Slug já cadastrado.");

        assertThat(service.create(tenantParaCriacao("tabira", " ", null), loggedUser).mensagem())
                .isEqualTo("Nome obrigatório.");
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void deveAtualizarTenantNormalizandoDominioEAuditoria() {
        Tenant persisted = tenant(1L, "afogados");
        Tenant update = tenant(1L, "ignorado");
        update.setName(" Novo Nome ");
        update.setDomain("SMS.EXEMPLO.COM:8080");
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(persisted));
        when(tenantRepository.save(persisted)).thenReturn(persisted);

        ResultadoOperacao<Tenant> resultado = service.update(update, loggedUser);

        assertThat(resultado.sucesso()).isTrue();
        assertThat(persisted.getName()).isEqualTo("Novo Nome");
        assertThat(persisted.getDomain()).isEqualTo("sms.exemplo.com");
        assertThat(persisted.getUpdateUser()).isEqualTo("root");
        assertThat(persisted.getUpdateDate()).isNotNull();
        verify(tenantRepository).save(persisted);
    }

    @Test
    void deveBloquearAtualizacaoTenantInexistenteDominioDuplicadoOuNomeVazio() {
        Tenant update = tenant(1L, "afogados");
        update.setName("Tenant");
        when(tenantRepository.findById(1L)).thenReturn(Optional.empty());
        assertThat(service.update(update, loggedUser).mensagem()).isEqualTo("Tenant não encontrado.");

        Tenant persisted = tenant(2L, "tabira");
        update.setId(2L);
        update.setDomain("duplicado.com");
        when(tenantRepository.findById(2L)).thenReturn(Optional.of(persisted));
        when(tenantRepository.existsByDomainAndIdNot("duplicado.com", 2L)).thenReturn(true);
        assertThat(service.update(update, loggedUser).mensagem()).isEqualTo("Domínio já cadastrado.");

        update.setDomain(null);
        update.setName(" ");
        assertThat(service.update(update, loggedUser).mensagem()).isEqualTo("Nome obrigatório.");
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void deveDesabilitarReativarEControlarManutencaoDoTenant() {
        Tenant toDisable = tenant(1L, "afogados");
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(toDisable));
        when(tenantRepository.save(toDisable)).thenReturn(toDisable);

        assertThat(service.disable(1L, " ajuste ", loggedUser).sucesso()).isTrue();
        assertThat(toDisable.getStatus()).isEqualTo(TenantStatus.DISABLED);
        assertThat(toDisable.getDisabledReason()).isEqualTo("ajuste");
        assertThat(toDisable.getDisabledUser()).isEqualTo("root");
        verify(tenantSessionService).expireTenantSessions(1L);

        Tenant toReactivate = tenant(2L, "tabira");
        toReactivate.setStatus(TenantStatus.DISABLED);
        toReactivate.setDisabledDate(LocalDateTime.now());
        toReactivate.setDisabledUser("old");
        toReactivate.setDisabledReason("old");
        when(tenantRepository.findById(2L)).thenReturn(Optional.of(toReactivate));
        when(tenantRepository.save(toReactivate)).thenReturn(toReactivate);

        assertThat(service.reactivate(2L, loggedUser).sucesso()).isTrue();
        assertThat(toReactivate.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(toReactivate.getDisabledDate()).isNull();
        assertThat(toReactivate.getDisabledUser()).isNull();
        assertThat(toReactivate.getDisabledReason()).isNull();

        Tenant toMaintain = tenant(3L, "iguaracy");
        when(tenantRepository.findById(3L)).thenReturn(Optional.of(toMaintain));
        when(tenantRepository.save(toMaintain)).thenReturn(toMaintain);

        assertThat(service.startMaintenance(3L, " migração ", loggedUser).sucesso()).isTrue();
        assertThat(toMaintain.getStatus()).isEqualTo(TenantStatus.MAINTENANCE);
        assertThat(toMaintain.getMaintenanceMessage()).isEqualTo("migração");
        assertThat(toMaintain.getMaintenanceUser()).isEqualTo("root");
        verify(tenantSessionService).expireTenantSessions(3L);

        assertThat(service.endMaintenance(3L, loggedUser).sucesso()).isTrue();
        assertThat(toMaintain.getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(toMaintain.getMaintenanceDate()).isNull();
        assertThat(toMaintain.getMaintenanceUser()).isNull();
        assertThat(toMaintain.getMaintenanceMessage()).isNull();
    }

    @Test
    void deveBloquearTransicoesInvalidasDoTenant() {
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.disable(99L, "x", loggedUser).mensagem()).isEqualTo("Tenant não encontrado.");
        assertThat(service.reactivate(99L, loggedUser).mensagem()).isEqualTo("Tenant não encontrado.");
        assertThat(service.startMaintenance(99L, "x", loggedUser).mensagem()).isEqualTo("Tenant não encontrado.");
        assertThat(service.endMaintenance(99L, loggedUser).mensagem()).isEqualTo("Tenant não encontrado.");

        Tenant disabled = tenant(1L, "afogados");
        disabled.setStatus(TenantStatus.DISABLED);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(disabled));
        assertThat(service.startMaintenance(1L, "x", loggedUser).mensagem())
                .isEqualTo("Tenant desabilitado não pode entrar em manutenção.");
        assertThat(service.endMaintenance(1L, loggedUser).mensagem())
                .isEqualTo("Tenant desabilitado não pode ser reativado por este fluxo.");
    }

    @Test
    void deveAtualizarSlugDominioEExpirarSessoesDoTenant() {
        Tenant tenant = tenant(1L, "antigo");
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(tenant)).thenReturn(tenant);

        ResultadoOperacao<Tenant> resultado = service.updateSlug(1L, " novo-slug ", "novo-slug", loggedUser);

        assertThat(resultado.sucesso()).isTrue();
        assertThat(tenant.getSlug()).isEqualTo("novo-slug");
        assertThat(tenant.getDomain()).isEqualTo("novo-slug.sigaubs.com.br");
        assertThat(tenant.getUpdateUser()).isEqualTo("root");
        verify(tenantSessionService).expireTenantSessions(1L);
    }

    @Test
    void deveValidarAtualizacaoDeSlug() {
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.updateSlug(99L, "novo", "novo", loggedUser).mensagem())
                .isEqualTo("Tenant não encontrado.");

        Tenant tenant = tenant(1L, "atual");
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        assertThat(service.updateSlug(1L, "-invalido", "-invalido", loggedUser).mensagem())
                .isEqualTo("Slug inválido.");
        assertThat(service.updateSlug(1L, "admin", "admin", loggedUser).mensagem())
                .isEqualTo("Slug reservado para o admin global.");
        assertThat(service.updateSlug(1L, "novo", "outro", loggedUser).mensagem())
                .isEqualTo("Confirmação do slug inválida.");
        assertThat(service.updateSlug(1L, "atual", "atual", loggedUser).valor()).isSameAs(tenant);

        when(tenantRepository.existsBySlugAndIdNot("novo", 1L)).thenReturn(true);
        assertThat(service.updateSlug(1L, "novo", "novo", loggedUser).mensagem())
                .isEqualTo("Slug já cadastrado.");
    }

    @Test
    void deveBuscarTenantPorIdEListarStatus() {
        Tenant tenant = tenant(1L, "afogados");
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.findById(1L).valor()).isSameAs(tenant);
        assertThat(service.findById(99L).mensagem()).isEqualTo("Tenant não encontrado.");
        assertThat(service.getStatuses()).contains(TenantStatus.ACTIVE, TenantStatus.DISABLED, TenantStatus.MAINTENANCE);
    }

    private Tenant tenantParaCriacao(String slug, String name, String domain) {
        Tenant tenant = new Tenant();
        tenant.setSlug(slug);
        tenant.setName(name);
        tenant.setDomain(domain);
        return tenant;
    }
}
