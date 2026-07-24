package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.dtos.UBSsystemUserDTO;
import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.Specialty;
import br.com.tecsus.sigaubs.services.BasicHealthUnitService;
import br.com.tecsus.sigaubs.services.SpecialtyService;
import br.com.tecsus.sigaubs.services.SystemUserService;
import br.com.tecsus.sigaubs.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.model;
import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.redirect;
import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.sms;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasicHealthUnitControllerTest {

    @Mock
    private BasicHealthUnitService basicHealthUnitService;
    @Mock
    private SystemUserService systemUserService;
    @Mock
    private SpecialtyService specialtyService;

    private BasicHealthUnit ubs;
    private Specialty specialty;
    private BasicHealthUnitController controller;

    @BeforeEach
    void setUp() {
        ubs = TestDataFactory.ubs(1L, "UBS Afogados");
        specialty = TestDataFactory.specialty(2L, "Cardiologia");
        controller = new BasicHealthUnitController(basicHealthUnitService, systemUserService, specialtyService);
    }

    @Test
    void deveAbrirPaginaDeUbs() {
        when(basicHealthUnitService.findAllUBS()).thenReturn(List.of(ubs));
        when(specialtyService.findSpecialties()).thenReturn(List.of(specialty));
        var model = model();

        assertThat(controller.getBasicHealthUnitPage(model, sms()))
                .isEqualTo("basicHealthUnitManagement/basicHealthUnit_management");
        assertThat(model.get("basicHealthUnits")).isEqualTo(List.of(ubs));
        assertThat(model.get("specialties")).isEqualTo(List.of(specialty));
        assertThat(model.get("ubsUsers")).isEqualTo(List.of());
    }

    @Test
    void deveCadastrarAtualizarETratarFalhas() throws Exception {
        var loggedUser = sms();
        var redirectAttributes = redirect();

        assertThat(controller.registerBasicHealthUnit(ubs, loggedUser, redirectAttributes))
                .isEqualTo("redirect:/basicHealthUnit-management");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(false);
        verify(basicHealthUnitService).registerBasicHealthUnit(ubs, loggedUser);

        doThrow(new RuntimeException("falha")).when(basicHealthUnitService)
                .registerBasicHealthUnit(ubs, loggedUser);
        redirectAttributes = redirect();
        controller.registerBasicHealthUnit(ubs, loggedUser, redirectAttributes);
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(true);

        redirectAttributes = redirect();
        controller.updateSystemUser(ubs, redirectAttributes, loggedUser);
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(false);
        verify(basicHealthUnitService).updateBasicHealthUnit(ubs, loggedUser);

        doThrow(new RuntimeException("falha")).when(basicHealthUnitService)
                .updateBasicHealthUnit(TestDataFactory.ubs(2L, "Outra"), loggedUser);
        redirectAttributes = redirect();
        controller.updateSystemUser(TestDataFactory.ubs(2L, "Outra"), redirectAttributes, loggedUser);
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(true);
    }

    @Test
    void devePrepararUbsParaEdicaoOuRedirecionarSemId() {
        assertThat(controller.getBasicHealthUnitPageToUpdate(null, model()))
                .isEqualTo("redirect:/basicHealthUnit-management");

        when(basicHealthUnitService.findSystemUserUBS(1L)).thenReturn(ubs);
        when(basicHealthUnitService.findAllUBS()).thenReturn(List.of(ubs));
        var model = model();

        assertThat(controller.getBasicHealthUnitPageToUpdate(1L, model))
                .isEqualTo("basicHealthUnitManagement/basicHealthUnit_management");
        assertThat(model.get("basicHealthUnit")).isSameAs(ubs);
        assertThat(model.get("basicHealthUnits")).isEqualTo(List.of(ubs));
    }

    @Test
    void deveListarUsuariosDaUbsOuTabelaVazia() {
        var model = model();

        assertThat(controller.getUBSsystemUsers(null, model))
                .isEqualTo("basicHealthUnitManagement/ubsFragments/emptySystemUsersUBSTable");
        assertThat(model.get("ubsUsers")).isEqualTo(List.of());

        when(basicHealthUnitService.findUBSsystemUsersByUBSid(1L)).thenReturn(List.of());
        model = model();
        assertThat(controller.getUBSsystemUsers(1L, model))
                .isEqualTo("basicHealthUnitManagement/ubsFragments/emptySystemUsersUBSTable");

        var user = new UBSsystemUserDTO(10L, "Maria", "Atendente", "true");
        when(basicHealthUnitService.findUBSsystemUsersByUBSid(2L)).thenReturn(List.of(user));
        model = model();
        assertThat(controller.getUBSsystemUsers(2L, model))
                .isEqualTo("basicHealthUnitManagement/ubsFragments/systemUsersUBSTable");
        assertThat(model.get("ubsUsers")).isEqualTo(List.of(user));
    }

    @Test
    void devePesquisarUsuariosParaVinculoSomenteAcimaDoLimite() {
        var model = model();
        assertThat(controller.getSystemUsersByLoggedUser("", model))
                .isEqualTo("basicHealthUnitManagement/ubsFragments/dropdownUserUBS");
        assertThat(model.get("users")).isEqualTo(List.of());

        model = model();
        controller.getSystemUsersByLoggedUser("ab", model);
        assertThat(model.get("users")).isEqualTo(List.of());

        var user = new UBSsystemUserDTO(10L, "Maria", "Atendente", "true");
        when(systemUserService.findSystemUserByNameContaining("Maria")).thenReturn(List.of(user));
        model = model();
        controller.getSystemUsersByLoggedUser("Maria", model);
        assertThat(model.get("users")).isEqualTo(List.of(user));
    }

    @Test
    void deveDeletarUbsOuIgnorarSemId() throws Exception {
        var loggedUser = sms();
        assertThat(controller.deleteBasicHealthUnit(null, loggedUser, redirect()))
                .isEqualTo("redirect:/basicHealthUnit-management");

        var redirectAttributes = redirect();
        controller.deleteBasicHealthUnit(1L, loggedUser, redirectAttributes);
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(false);
        verify(basicHealthUnitService).deleteBasicHealtUnit(1L, loggedUser);

        doThrow(new RuntimeException("falha")).when(basicHealthUnitService).deleteBasicHealtUnit(2L, loggedUser);
        redirectAttributes = redirect();
        controller.deleteBasicHealthUnit(2L, loggedUser, redirectAttributes);
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(true);
    }

    @Test
    void deveDesvincularEVincularUsuarioAtualizandoTabela() throws Exception {
        var loggedUser = sms();
        var user = new UBSsystemUserDTO(10L, "Maria", "Atendente", "true");
        when(basicHealthUnitService.findUBSsystemUsersByUBSid(1L)).thenReturn(List.of(user), List.of());
        var model = model();

        assertThat(controller.unlinkBasicHealthUnitSystemUser(10L, 1L, loggedUser, model))
                .isEqualTo("basicHealthUnitManagement/ubsFragments/systemUsersUBSTable");
        assertThat(model.get("attach_error")).isEqualTo(false);
        verify(basicHealthUnitService).unlinkBasicHealthUnitSystemUser(10L, loggedUser);

        model = model();
        assertThat(controller.appendSystemUserToBasicHealthUnit("Maria", 1L, 10L, model))
                .isEqualTo("basicHealthUnitManagement/ubsFragments/emptySystemUsersUBSTable");
        assertThat(model.get("attach_error")).isEqualTo(false);
        verify(basicHealthUnitService).attachSystemUserToUBS(10L, 1L);
    }

    @Test
    void deveRegistrarErroAoVincularOuDesvincularUsuario() throws Exception {
        var loggedUser = sms();
        doThrow(new RuntimeException("falha")).when(basicHealthUnitService)
                .unlinkBasicHealthUnitSystemUser(10L, loggedUser);
        when(basicHealthUnitService.findUBSsystemUsersByUBSid(1L)).thenReturn(List.of());
        var model = model();

        assertThat(controller.unlinkBasicHealthUnitSystemUser(10L, 1L, loggedUser, model))
                .isEqualTo("basicHealthUnitManagement/ubsFragments/emptySystemUsersUBSTable");
        assertThat(model.get("attach_error")).isEqualTo(true);

        doThrow(new RuntimeException("falha")).when(basicHealthUnitService).attachSystemUserToUBS(11L, 1L);
        model = model();
        controller.appendSystemUserToBasicHealthUnit("Joao", 1L, 11L, model);
        assertThat(model.get("attach_error")).isEqualTo(true);
    }
}
