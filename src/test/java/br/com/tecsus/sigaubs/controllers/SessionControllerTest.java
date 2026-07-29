package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.dtos.DashboardDTO;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.dtos.UBSDashboardDTO;
import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.SystemRole;
import br.com.tecsus.sigaubs.entities.SystemUser;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.services.BasicHealthUnitService;
import br.com.tecsus.sigaubs.services.DashboardService;
import br.com.tecsus.sigaubs.services.SystemUserService;
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
    void devePrepararUsuarioParaEdicao() {
        var loggedUser = sms();
        when(systemUserService.findManageableSystemUserById(3L, loggedUser)).thenReturn(user);
        when(systemUserService.getRolesNotAdminAndNotManagement()).thenReturn(List.of(role));
        when(basicHealthUnitService.findAllUBS()).thenReturn(List.of(ubs));
        when(systemUserService.findAllUsersByCreationUserPaginated(any(SystemUser.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(user)));
        var model = model();

        assertThat(controller.getSystemUserInsertPageToUpdate(3L, model, loggedUser))
                .isEqualTo("sessionManagement/systemUser_management");
        assertThat(model.get("systemUser")).isSameAs(user);
    }

}
