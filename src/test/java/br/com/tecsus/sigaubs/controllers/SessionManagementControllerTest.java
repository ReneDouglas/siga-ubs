package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.dtos.SessionSummaryDTO;
import br.com.tecsus.sigaubs.entities.SystemAdmin;
import br.com.tecsus.sigaubs.entities.SystemUser;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.exceptions.ResourceNotFoundException;
import br.com.tecsus.sigaubs.security.SessionMetadata;
import br.com.tecsus.sigaubs.services.AdminSmsUserService;
import br.com.tecsus.sigaubs.services.AdminUserManagementService;
import br.com.tecsus.sigaubs.services.SystemUserService;
import br.com.tecsus.sigaubs.services.TenantSessionService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static br.com.tecsus.sigaubs.support.TestDataFactory.role;
import static br.com.tecsus.sigaubs.support.TestDataFactory.systemAdmin;
import static br.com.tecsus.sigaubs.support.TestDataFactory.systemUser;
import static br.com.tecsus.sigaubs.support.TestDataFactory.userDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionManagementControllerTest {

    private static final String MANAGEMENT_ID =
            "10000000-0000-0000-0000-000000000001";

    @Mock
    private TenantSessionService tenantSessionService;
    @Mock
    private SystemUserService systemUserService;
    @Mock
    private AdminSmsUserService adminSmsUserService;
    @Mock
    private AdminUserManagementService adminUserManagementService;

    private SessionManagementController controller;

    @BeforeEach
    void setUp() {
        controller = new SessionManagementController(
                tenantSessionService,
                systemUserService,
                adminSmsUserService,
                adminUserManagementService);
    }

    @Test
    void deveExibirERevogarAsPropriasSessoesSemExporIdReal() {
        var loggedUser = userDetails(
                "user", "Usuário", 7L, 1L, "afogados", Roles.ROLE_USER);
        HttpSession currentSession = mock(HttpSession.class);
        when(currentSession.getId()).thenReturn("id-real-da-sessao");
        when(tenantSessionService.listOwnSessions(
                loggedUser, "id-real-da-sessao"))
                .thenReturn(List.of(summary(true)));
        var model = new ExtendedModelMap();

        assertThat(controller.ownTenantSessions(loggedUser, currentSession, model))
                .isEqualTo("sessionManagement/session_management");
        assertThat(model.get("sessions")).isEqualTo(List.of(summary(true)));
        assertThat(model.get("revokeBaseUrl")).isEqualTo("/session-management");

        when(currentSession.getAttribute(SessionMetadata.MANAGEMENT_ID_ATTRIBUTE))
                .thenReturn(MANAGEMENT_ID);
        var redirect = new RedirectAttributesModelMap();
        assertThat(controller.revokeOwnTenantSession(
                MANAGEMENT_ID, loggedUser, currentSession, redirect))
                .isEqualTo("redirect:/session-management");
        assertThat(redirect.getFlashAttributes().get("error")).isEqualTo(false);
        verify(currentSession).invalidate();
    }

    @Test
    void smsDeveGerenciarSomenteUsuarioMunicipalAutorizado() {
        var loggedUser = userDetails(
                "sms", "Coordenador", null, 1L, "afogados", Roles.ROLE_SMS);
        SystemUser target = systemUser(7L, "atendente", role(2L, Roles.ROLE_ATENDENTE));
        when(systemUserService.findManageableSystemUserById(7L, loggedUser))
                .thenReturn(target);
        var model = new ExtendedModelMap();

        assertThat(controller.managedTenantUserSessions(7L, loggedUser, model))
                .isEqualTo("sessionManagement/session_management");
        verify(tenantSessionService).listSessions(
                "tenant:1:user:7", null);

        when(tenantSessionService.revokeSessionForPrincipal(
                "tenant:1:user:7", MANAGEMENT_ID)).thenReturn(true);
        assertThat(controller.revokeManagedTenantUserSession(
                7L,
                MANAGEMENT_ID,
                loggedUser,
                new RedirectAttributesModelMap()))
                .isEqualTo("redirect:/systemUser-management/7/sessions");

        when(systemUserService.findManageableSystemUserById(8L, loggedUser))
                .thenReturn(null);
        assertThatThrownBy(() ->
                controller.managedTenantUserSessions(
                        8L, loggedUser, new ExtendedModelMap()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void adminGlobalDeveGerenciarAdminsESmsDoTenantInformado() {
        var loggedAdmin = userDetails(
                "admin", "Admin", null, null, null, Roles.ROLE_ADMIN);
        SystemAdmin targetAdmin = systemAdmin("outro-admin", "hash");
        targetAdmin.setId(9L);
        when(adminUserManagementService.findById(9L))
                .thenReturn(ResultadoOperacao.sucesso(targetAdmin));
        HttpSession currentSession = mock(HttpSession.class);
        var model = new ExtendedModelMap();

        assertThat(controller.managedAdminSessions(
                9L, loggedAdmin, currentSession, model))
                .isEqualTo("sessionManagement/session_management");
        verify(tenantSessionService).listSessions("admin:9", null);
        when(tenantSessionService.revokeSessionForPrincipal(
                "admin:9", MANAGEMENT_ID)).thenReturn(true);
        assertThat(controller.revokeManagedAdminSession(
                9L,
                MANAGEMENT_ID,
                loggedAdmin,
                currentSession,
                new RedirectAttributesModelMap()))
                .isEqualTo("redirect:/admin/admin-user-management/9/sessions");

        SystemUser targetSms = systemUser(
                10L, "sms-tenant", role(4L, Roles.ROLE_SMS));
        when(adminSmsUserService.findSmsUser(2L, 10L)).thenReturn(targetSms);
        model = new ExtendedModelMap();

        assertThat(controller.managedSmsSessions(2L, 10L, model))
                .isEqualTo("sessionManagement/session_management");
        verify(tenantSessionService).listSessions("tenant:2:user:10", null);
        when(tenantSessionService.revokeSessionForPrincipal(
                "tenant:2:user:10", MANAGEMENT_ID)).thenReturn(true);
        assertThat(controller.revokeManagedSmsSession(
                2L,
                10L,
                MANAGEMENT_ID,
                new RedirectAttributesModelMap()))
                .isEqualTo(
                        "redirect:/admin/tenant-management/2/sms-users/10/sessions");
    }

    @Test
    void adminGlobalDeveExibirERevogarTodasAsSessoesAtivas() {
        var loggedAdmin = new br.com.tecsus.sigaubs.security.SystemUserDetails(
                5L,
                "admin",
                "{noop}12345678",
                Set.of(new SimpleGrantedAuthority(Roles.ROLE_ADMIN.toString())),
                "Admin",
                "admin@example.com",
                true,
                null,
                null,
                null);
        HttpSession currentSession = mock(HttpSession.class);
        when(currentSession.getId()).thenReturn("sessao-admin-real");
        when(tenantSessionService.listAllSessions("sessao-admin-real"))
                .thenReturn(List.of(summary(true)));
        var model = new ExtendedModelMap();

        assertThat(controller.activeAdminSessions(
                loggedAdmin, currentSession, model))
                .isEqualTo("sessionManagement/session_management");
        assertThat(model.get("adminArea")).isEqualTo(true);
        assertThat(model.get("ownSessions")).isEqualTo(false);
        assertThat(model.get("globalSessions")).isEqualTo(true);

        when(currentSession.getAttribute(SessionMetadata.MANAGEMENT_ID_ATTRIBUTE))
                .thenReturn(MANAGEMENT_ID);
        assertThat(controller.revokeAdminSession(
                MANAGEMENT_ID,
                currentSession,
                new RedirectAttributesModelMap()))
                .isEqualTo("redirect:/admin/session-management");
        verify(currentSession).invalidate();

        when(currentSession.getAttribute(SessionMetadata.MANAGEMENT_ID_ATTRIBUTE))
                .thenReturn("10000000-0000-0000-0000-000000000099");
        when(tenantSessionService.revokeAnySession(MANAGEMENT_ID))
                .thenReturn(true);
        var redirect = new RedirectAttributesModelMap();
        assertThat(controller.revokeAdminSession(
                MANAGEMENT_ID,
                currentSession,
                redirect))
                .isEqualTo("redirect:/admin/session-management");
        assertThat(redirect.getFlashAttributes().get("error")).isEqualTo(false);
        verify(tenantSessionService).revokeAnySession(MANAGEMENT_ID);
    }

    private SessionSummaryDTO summary(boolean current) {
        return new SessionSummaryDTO(
                MANAGEMENT_ID,
                Instant.parse("2026-07-29T10:00:00Z"),
                Instant.parse("2026-07-29T11:00:00Z"),
                Duration.ofMinutes(30),
                "Chrome · computador",
                current);
    }
}
