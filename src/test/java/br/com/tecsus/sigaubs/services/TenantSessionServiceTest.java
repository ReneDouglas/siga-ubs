package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.SessionSummaryDTO;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.security.SessionMetadata;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantSessionServiceTest {

    @Test
    void deveRevogarSessoesDoTenantNoJdbcENoRegistroLocal() {
        SessionRegistry registry = mock(SessionRegistry.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        @SuppressWarnings("unchecked")
        FindByIndexNameSessionRepository<Session> repository =
                mock(FindByIndexNameSessionRepository.class);
        TenantSessionService service = new TenantSessionService(registry, repository, jdbc);
        SystemUserDetails tenantOne = tenantUser(10L, 1L);
        SystemUserDetails tenantTwo = tenantUser(20L, 2L);
        SessionInformation active = mock(SessionInformation.class);
        SessionInformation expired = mock(SessionInformation.class);
        when(expired.isExpired()).thenReturn(true);
        when(registry.getAllPrincipals()).thenReturn(List.of("ignorado", tenantOne, tenantTwo));
        when(registry.getAllSessions(tenantOne, false)).thenReturn(List.of(active, expired));

        service.expireTenantSessions(null);
        verify(jdbc, never()).update(
                "DELETE FROM SPRING_SESSION WHERE PRINCIPAL_NAME LIKE ?", "tenant:null:user:%");

        service.expireTenantSessions(1L);

        verify(jdbc).update(
                "DELETE FROM SPRING_SESSION WHERE PRINCIPAL_NAME LIKE ?",
                "tenant:1:user:%");
        verify(active).expireNow();
        verify(expired, never()).expireNow();
        verify(registry, never()).getAllSessions(tenantTwo, false);
    }

    @Test
    void deveRevogarTodasAsSessoesTenantSemRevogarAdmin() {
        SessionRegistry registry = mock(SessionRegistry.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        @SuppressWarnings("unchecked")
        FindByIndexNameSessionRepository<Session> repository =
                mock(FindByIndexNameSessionRepository.class);
        TenantSessionService service = new TenantSessionService(registry, repository, jdbc);
        SystemUserDetails tenant = tenantUser(10L, 1L);
        SystemUserDetails admin = adminUser(30L);
        SessionInformation active = mock(SessionInformation.class);
        when(registry.getAllPrincipals()).thenReturn(List.of(tenant, admin));
        when(registry.getAllSessions(tenant, false)).thenReturn(List.of(active));

        service.expireTenantScopedSessions();

        verify(jdbc).update(
                "DELETE FROM SPRING_SESSION WHERE PRINCIPAL_NAME LIKE ?",
                "tenant:%");
        verify(active).expireNow();
        verify(registry, never()).getAllSessions(admin, false);
    }

    @Test
    void deveListarOrdenadoEPermitirRevogarSomenteSessaoDoProprioPrincipal() {
        SessionRegistry registry = mock(SessionRegistry.class);
        @SuppressWarnings("unchecked")
        FindByIndexNameSessionRepository<Session> repository =
                mock(FindByIndexNameSessionRepository.class);
        TenantSessionService service = new TenantSessionService(registry, repository, null);
        String olderManagementId = "10000000-0000-0000-0000-000000000001";
        String currentManagementId = "10000000-0000-0000-0000-000000000002";
        Session older = session(
                "old", olderManagementId, Instant.parse("2026-07-29T10:00:00Z"));
        Session current = session(
                "current", currentManagementId, Instant.parse("2026-07-29T11:00:00Z"));
        when(repository.findByPrincipalName("tenant:1:user:10"))
                .thenReturn(Map.of("old", older, "current", current));

        List<SessionSummaryDTO> result =
                service.listSessions("tenant:1:user:10", "current");

        assertThat(result).extracting(SessionSummaryDTO::managementId)
                .containsExactly(currentManagementId, olderManagementId);
        assertThat(result.getFirst().current()).isTrue();
        assertThat(result.getFirst().clientDescription()).isEqualTo("Chrome · computador");
        assertThat(result.getFirst().locationDescription())
                .isEqualTo("Afogados da Ingazeira, PE · BR");
        assertThat(service.revokeSessionForPrincipal(
                "tenant:1:user:10",
                "10000000-0000-0000-0000-000000000099")).isFalse();
        assertThat(service.revokeSessionForPrincipal("tenant:1:user:10", null)).isFalse();
        assertThat(service.revokeSessionForPrincipal(
                "tenant:1:user:10", olderManagementId)).isTrue();
        verify(repository).deleteById("old");
    }

    @Test
    void deveExibirFallbackParaSessaoCriadaAntesDaColetaDeLocalidade() {
        SessionRegistry registry = mock(SessionRegistry.class);
        @SuppressWarnings("unchecked")
        FindByIndexNameSessionRepository<Session> repository =
                mock(FindByIndexNameSessionRepository.class);
        TenantSessionService service =
                new TenantSessionService(registry, repository, null);
        Session legacySession = session(
                "legacy",
                "10000000-0000-0000-0000-000000000003",
                Instant.parse("2026-07-29T11:00:00Z"));
        when(legacySession.getAttribute(
                SessionMetadata.LOCATION_DESCRIPTION_ATTRIBUTE)).thenReturn(null);
        when(repository.findByPrincipalName("tenant:1:user:10"))
                .thenReturn(Map.of("legacy", legacySession));

        List<SessionSummaryDTO> result =
                service.listSessions("tenant:1:user:10", "legacy");

        assertThat(result).singleElement()
                .extracting(SessionSummaryDTO::locationDescription)
                .isEqualTo(SessionMetadata.LOCATION_UNAVAILABLE);
    }

    @Test
    void deveRevogarPorChavesEstaveisETratarRepositorioIndisponivel() {
        SessionRegistry registry = mock(SessionRegistry.class);
        @SuppressWarnings("unchecked")
        FindByIndexNameSessionRepository<Session> repository =
                mock(FindByIndexNameSessionRepository.class);
        TenantSessionService service = new TenantSessionService(registry, repository, null);
        when(repository.findByPrincipalName("tenant:1:user:10"))
                .thenReturn(Map.of("tenant-session", mock(Session.class)));
        when(repository.findByPrincipalName("admin:30"))
                .thenReturn(Map.of("admin-session", mock(Session.class)));

        service.expireTenantUserSessions(1L, 10L);
        service.expireAdminUserSessions(30L);
        service.expireTenantUserSessions(1L, null);

        verify(repository).deleteById("tenant-session");
        verify(repository).deleteById("admin-session");

        TenantSessionService localOnly = new TenantSessionService(registry);
        assertThat(localOnly.listSessions("principal", "session")).isEmpty();
        assertThat(localOnly.revokeSessionForPrincipal(
                "principal", "10000000-0000-0000-0000-000000000001")).isFalse();
        localOnly.expireAdminUserSessions(30L);
    }

    @Test
    void smsDeveListarERevogarSomenteAsPropriasSessoesDoTenant() {
        SessionRegistry registry = mock(SessionRegistry.class);
        @SuppressWarnings("unchecked")
        FindByIndexNameSessionRepository<Session> repository =
                mock(FindByIndexNameSessionRepository.class);
        TenantSessionService service =
                new TenantSessionService(registry, repository, null);
        SystemUserDetails sms = smsUser(10L, 1L);
        String ownManagementId =
                "10000000-0000-0000-0000-000000000010";
        String anotherSmsManagementId =
                "10000000-0000-0000-0000-000000000020";
        Session ownSession = session(
                "sms-afogados",
                ownManagementId,
                Instant.parse("2026-07-29T12:00:00Z"));
        when(repository.findByPrincipalName("tenant:1:user:10"))
                .thenReturn(Map.of("sms-afogados", ownSession));

        List<SessionSummaryDTO> result =
                service.listOwnSessions(sms, "sms-afogados");

        assertThat(result)
                .extracting(SessionSummaryDTO::managementId)
                .containsExactly(ownManagementId);
        assertThat(service.revokeOwnSession(
                sms, anotherSmsManagementId)).isFalse();
        assertThat(service.revokeOwnSession(
                sms, ownManagementId)).isTrue();
        verify(repository, never()).findByPrincipalName("sms");
        verify(repository, never()).findByPrincipalName("tenant:2:user:20");
        verify(repository).deleteById("sms-afogados");
    }

    @Test
    void adminGlobalDeveListarERevogarSessoesDeTodosOsPrincipals() {
        SessionRegistry registry = mock(SessionRegistry.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        @SuppressWarnings("unchecked")
        FindByIndexNameSessionRepository<Session> repository =
                mock(FindByIndexNameSessionRepository.class);
        TenantSessionService service =
                new TenantSessionService(registry, repository, jdbc);
        String adminManagementId =
                "10000000-0000-0000-0000-000000000030";
        String smsManagementId =
                "10000000-0000-0000-0000-000000000010";
        Session adminSession = session(
                "admin-session",
                adminManagementId,
                Instant.parse("2026-07-29T13:00:00Z"));
        Session smsSession = session(
                "sms-session",
                smsManagementId,
                Instant.parse("2026-07-29T12:00:00Z"));
        addOwner(adminSession, adminUser(30L));
        addOwner(smsSession, smsUser(10L, 1L));
        when(jdbc.queryForList(
                anyString(), eq(String.class), anyLong()))
                .thenReturn(List.of(
                        "tenant:1:user:10",
                        "chave-invalida",
                        "admin:30"));
        when(repository.findByPrincipalName("admin:30"))
                .thenReturn(Map.of("admin-session", adminSession));
        when(repository.findByPrincipalName("tenant:1:user:10"))
                .thenReturn(Map.of("sms-session", smsSession));

        List<SessionSummaryDTO> result =
                service.listAllSessions("admin-session");

        assertThat(result)
                .extracting(SessionSummaryDTO::managementId)
                .containsExactly(adminManagementId, smsManagementId);
        assertThat(result)
                .extracting(SessionSummaryDTO::ownerDescription)
                .containsExactly(
                        "Admin (admin) · ADMIN",
                        "SMS (sms) · SMS · tenant");
        assertThat(result.getFirst().current()).isTrue();
        assertThat(service.revokeAnySession(smsManagementId)).isTrue();
        verify(repository).deleteById("sms-session");
        verify(repository, never()).findByPrincipalName("chave-invalida");
    }

    private Session session(String id, String managementId, Instant lastAccess) {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn(id);
        when(session.getCreationTime()).thenReturn(lastAccess.minus(Duration.ofHours(1)));
        when(session.getLastAccessedTime()).thenReturn(lastAccess);
        when(session.getMaxInactiveInterval()).thenReturn(Duration.ofMinutes(30));
        when(session.getAttribute(SessionMetadata.MANAGEMENT_ID_ATTRIBUTE))
                .thenReturn(managementId);
        when(session.getAttribute(SessionMetadata.CLIENT_DESCRIPTION_ATTRIBUTE))
                .thenReturn("Chrome · computador");
        when(session.getAttribute(SessionMetadata.LOCATION_DESCRIPTION_ATTRIBUTE))
                .thenReturn("Afogados da Ingazeira, PE · BR");
        return session;
    }

    private void addOwner(Session session, SystemUserDetails owner) {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(session.getAttribute(
                org.springframework.security.web.context.HttpSessionSecurityContextRepository
                        .SPRING_SECURITY_CONTEXT_KEY))
                .thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(owner);
    }

    private SystemUserDetails tenantUser(Long id, Long tenantId) {
        return new SystemUserDetails(
                id,
                "user-" + id,
                "{noop}senha",
                List.of(new SimpleGrantedAuthority(Roles.ROLE_USER.toString())),
                "Usuário",
                "user@example.com",
                true,
                7L,
                tenantId,
                "tenant");
    }

    private SystemUserDetails adminUser(Long id) {
        return new SystemUserDetails(
                id,
                "admin",
                "{noop}senha",
                List.of(new SimpleGrantedAuthority(Roles.ROLE_ADMIN.toString())),
                "Admin",
                "admin@example.com",
                true,
                null,
                null,
                null);
    }

    private SystemUserDetails smsUser(Long id, Long tenantId) {
        return new SystemUserDetails(
                id,
                "sms",
                "{noop}senha",
                List.of(new SimpleGrantedAuthority(Roles.ROLE_SMS.toString())),
                "SMS",
                "sms@example.com",
                true,
                null,
                tenantId,
                "tenant");
    }
}
