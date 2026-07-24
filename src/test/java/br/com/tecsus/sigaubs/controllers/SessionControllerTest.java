package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.dtos.DashboardDTO;
import br.com.tecsus.sigaubs.dtos.UBSDashboardDTO;
import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.SystemRole;
import br.com.tecsus.sigaubs.entities.SystemUser;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.services.BasicHealthUnitService;
import br.com.tecsus.sigaubs.services.DashboardService;
import br.com.tecsus.sigaubs.services.SystemUserService;
import br.com.tecsus.sigaubs.services.exceptions.InvalidConfirmPasswordException;
import br.com.tecsus.sigaubs.support.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.admin;
import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.model;
import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.redirect;
import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.sms;
import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.ubsUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    @Mock
    private SystemUserService systemUserService;
    @Mock
    private BasicHealthUnitService basicHealthUnitService;
    @Mock
    private DashboardService dashboardService;

    private BasicHealthUnit ubs;
    private SystemRole role;
    private SystemUser user;
    private SessionController controller;

    @BeforeEach
    void setUp() {
        ubs = TestDataFactory.ubs(1L, "UBS Afogados");
        role = TestDataFactory.role(2L, Roles.ROLE_ATENDENTE);
        user = TestDataFactory.systemUser(3L, "maria", role);
        controller = new SessionController(systemUserService, basicHealthUnitService, dashboardService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveMontarHomePorPerfil() {
        DashboardDTO dashboard = new DashboardDTO(List.of(), List.of(), List.of(), List.of(), List.of(),
                null, List.of(), List.of(), List.of());
        when(dashboardService.loadDashboardData()).thenReturn(dashboard);
        var model = model();

        assertThat(controller.getHomePage(model, admin())).isEqualTo("home");
        assertThat(model.get("dashboard")).isSameAs(dashboard);

        UBSDashboardDTO ubsDashboard = new UBSDashboardDTO("UBS", 1L, 2L, 3L, List.of());
        when(dashboardService.loadUBSDashboardData(1L)).thenReturn(ubsDashboard);
        model = model();
        assertThat(controller.getHomePage(model, ubsUser(1L))).isEqualTo("home");
        assertThat(model.get("ubsDashboard")).isSameAs(ubsDashboard);

        model = model();
        assertThat(controller.getHomePage(model, ubsUser(null))).isEqualTo("home");
        assertThat(model).doesNotContainKey("ubsDashboard");
    }

    @Test
    void deveRetornarTelasSimplesDeSessao() {
        assertThat(controller.getLoginPage()).isEqualTo("sessionManagement/login");
        assertThat(controller.getErrorPage()).isEqualTo("error");
        assertThat(controller.getExpiredPage()).isEqualTo("expired");

        var model = model();
        assertThat(controller.getLoginErrorPage(model)).isEqualTo("sessionManagement/login");
        assertThat(model.get("loginError")).isEqualTo(true);
    }

    @Test
    void deveAbrirManutencaoDeUsuariosEAtenderBuscaAjax() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("sms", "senha", List.of()));
        when(systemUserService.getRolesNotAdminAndNotManagement()).thenReturn(List.of(role));
        when(basicHealthUnitService.findAllUBS()).thenReturn(List.of(ubs));
        when(systemUserService.findAllUsersByCreationUserPaginated(any(SystemUser.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        var request = new MockHttpServletRequest();
        var model = model();
        assertThat(controller.getSystemUserInsertPage(model, new SystemUser(), 0, 10, request))
                .isEqualTo("sessionManagement/systemUser_management");
        assertThat(model.get("rolesList")).isEqualTo(List.of(role));
        assertThat(model.get("basicHealthUnits")).isEqualTo(List.of(ubs));

        request = new MockHttpServletRequest();
        request.addHeader("X-Requested-With", "searchRequest");
        assertThat(controller.getSystemUserInsertPage(model(), new SystemUser(), 0, 10, request))
                .isEqualTo("sessionManagement/sessionFragments/systemUser_datatable");
    }

    @Test
    void devePrepararUsuarioParaEdicao() {
        when(systemUserService.findSystemUserById(3L)).thenReturn(user);
        when(systemUserService.getRolesNotAdminAndNotManagement()).thenReturn(List.of(role));
        when(basicHealthUnitService.findAllUBS()).thenReturn(List.of(ubs));
        when(systemUserService.findAllUsersByCreationUserPaginated(any(SystemUser.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(user)));
        var model = model();

        assertThat(controller.getSystemUserInsertPageToUpdate(3L, model, sms()))
                .isEqualTo("sessionManagement/systemUser_management");
        assertThat(model.get("systemUser")).isSameAs(user);
    }

    @Test
    void deveCadastrarUsuarioETratarErros() throws Exception {
        var loggedUser = sms();
        var redirectAttributes = redirect();

        controller.registerSystemUser(user, loggedUser, redirectAttributes);
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(false);
        verify(systemUserService).registerNotAdminSystemUser(user, loggedUser);

        doThrow(new DataIntegrityViolationException("duplicado")).when(systemUserService)
                .registerNotAdminSystemUser(user, admin());
        redirectAttributes = redirect();
        controller.registerSystemUser(user, admin(), redirectAttributes);
        assertThat(redirectAttributes.getFlashAttributes().get("message"))
                .isEqualTo("Usuário já cadastrado no sistema.");

        var invalidUser = ubsUser(1L);
        doThrow(new InvalidConfirmPasswordException("senha")).when(systemUserService)
                .registerNotAdminSystemUser(user, invalidUser);
        redirectAttributes = redirect();
        controller.registerSystemUser(user, invalidUser, redirectAttributes);
        assertThat(redirectAttributes.getFlashAttributes().get("message")).isEqualTo("As senhas não conferem.");

        var genericUser = TestDataFactory.userDetails("generic", "Generic", null, 1L, "afogados", Roles.ROLE_SMS);
        doThrow(new RuntimeException("falha")).when(systemUserService)
                .registerNotAdminSystemUser(user, genericUser);
        redirectAttributes = redirect();
        controller.registerSystemUser(user, genericUser, redirectAttributes);
        assertThat(redirectAttributes.getFlashAttributes().get("message")).isEqualTo("Erro ao cadastrar usuário.");
    }

    @Test
    void deveAtualizarEDeletarUsuarioComTratamentoDeErro() throws Exception {
        var redirectAttributes = redirect();

        controller.updateSystemUser(user, redirectAttributes);
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(false);
        verify(systemUserService).updateNotAdminSystemUser(user);

        doThrow(new RuntimeException("falha")).when(systemUserService)
                .updateNotAdminSystemUser(TestDataFactory.systemUser(4L, "joao", role));
        redirectAttributes = redirect();
        controller.updateSystemUser(TestDataFactory.systemUser(4L, "joao", role), redirectAttributes);
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(true);

        redirectAttributes = redirect();
        controller.deleteSystemUser(3L, redirectAttributes);
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(false);
        verify(systemUserService).deleteNotAdminSystemUser(3L);

        doThrow(new RuntimeException("falha")).when(systemUserService).deleteNotAdminSystemUser(4L);
        redirectAttributes = redirect();
        controller.deleteSystemUser(4L, redirectAttributes);
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(true);
    }

    @Test
    void deveValidarSenhaDoUsuario() {
        var loggedUser = sms();
        when(systemUserService.validateSystemUserByPassword("ok", loggedUser)).thenReturn(true);
        when(systemUserService.validateSystemUserByPassword("bad", loggedUser)).thenReturn(false);
        when(systemUserService.validateSystemUserByPassword("erro", loggedUser)).thenThrow(new RuntimeException("falha"));

        assertThat(controller.validateSystemUserByPassword("ok", loggedUser).getStatusCode()).isEqualTo(HttpStatus.OK);
        var invalid = controller.validateSystemUserByPassword("bad", loggedUser);
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalid.getBody()).isEqualTo("Senha inválida");

        var error = controller.validateSystemUserByPassword("erro", loggedUser);
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(error.getBody()).isEqualTo("Erro ao validar senha.");
    }
}
