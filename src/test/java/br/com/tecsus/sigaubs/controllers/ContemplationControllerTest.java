package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.Contemplation;
import br.com.tecsus.sigaubs.entities.Specialty;
import br.com.tecsus.sigaubs.enums.AppointmentStatus;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.services.BasicHealthUnitService;
import br.com.tecsus.sigaubs.services.ContemplationService;
import br.com.tecsus.sigaubs.services.SpecialtyService;
import br.com.tecsus.sigaubs.services.exceptions.CancelContemplationException;
import br.com.tecsus.sigaubs.services.exceptions.ConfirmContemplationException;
import br.com.tecsus.sigaubs.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.admin;
import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.model;
import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.redirect;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContemplationControllerTest {

    @Mock
    private BasicHealthUnitService basicHealthUnitService;
    @Mock
    private SpecialtyService specialtyService;
    @Mock
    private ContemplationService contemplationService;

    private BasicHealthUnit ubs;
    private Specialty specialty;
    private Contemplation contemplation;
    private ContemplationController controller;

    @BeforeEach
    void setUp() {
        ubs = TestDataFactory.ubs(1L, "UBS Afogados");
        specialty = TestDataFactory.specialty(2L, "Cardiologia");
        var procedure = TestDataFactory.procedure(3L, "Consulta", ProcedureType.CONSULTA, specialty);
        var patient = TestDataFactory.patient(4L, "Maria", ubs);
        var appointment = TestDataFactory.appointment(5L, patient, procedure);
        contemplation = TestDataFactory.contemplation(6L, appointment,
                TestDataFactory.slot(7L, ubs, procedure, 10, 5));
        when(basicHealthUnitService.findAllUBS()).thenReturn(List.of(ubs));
        when(specialtyService.findSpecialties()).thenReturn(List.of(specialty));
        controller = new ContemplationController(basicHealthUnitService, specialtyService, contemplationService);
    }

    @Test
    void deveAbrirPaginaDeContemplacoesComAbasIniciais() {
        when(contemplationService.findContemplationsByUBSAndSpecialty(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(contemplation)));
        var model = model();

        assertThat(controller.getContemplationPage(model))
                .isEqualTo("contemplationManagement/contemplation_management");
        assertThat(model.get("basicHealthUnits")).isEqualTo(List.of(ubs));
        assertThat(model.get("specialties")).isEqualTo(List.of(specialty));
        assertThat(model.get("consultasPage")).isInstanceOf(PageImpl.class);
        assertThat(model.get("hide")).isEqualTo("hidden");
    }

    @Test
    void devePesquisarContemplacoesComFiltros() {
        when(contemplationService.findContemplationsByUBSAndSpecialty(any(), eq(1L), eq(2L),
                eq("2026-07"), eq("PRESENCA_CONFIRMADA"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(contemplation)));
        var model = model();

        assertThat(controller.loadSearchContemplations(1L, 2L, "2026-07", "PRESENCA_CONFIRMADA", model))
                .isEqualTo("contemplationManagement/contemplation_management");
        assertThat(model.get("selectedUBS")).isEqualTo(1L);
        assertThat(model.get("selectedSpecialty")).isEqualTo(2L);
        assertThat(model.get("selectedMonth")).isEqualTo("2026-07");
        assertThat(model.get("selectedStatus")).isEqualTo("PRESENCA_CONFIRMADA");
    }

    @Test
    void devePaginarContemplacoesPorTipo() {
        when(contemplationService.findContemplationsByUBSAndSpecialty(eq(ProcedureType.CONSULTA), eq(1L), eq(2L),
                eq("2026-07"), eq("PRESENCA_CONFIRMADA"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(contemplation)));

        var model = model();
        assertThat(controller.getMedicalSlotsPaginated(model, 0, 10, 10, 10, 1L, 2L,
                "2026-07", "CONSULTA", "PRESENCA_CONFIRMADA"))
                .isEqualTo("contemplationManagement/contemplationFragments/consultas_datatable");
        assertThat(model.get("consultasPage")).isInstanceOf(PageImpl.class);

        controller.getMedicalSlotsPaginated(model(), 0, 10, 10, 10, 1L, 2L,
                "2026-07", "EXAME", "PRESENCA_CONFIRMADA");
        verify(contemplationService).findContemplationsByUBSAndSpecialty(eq(ProcedureType.EXAME), eq(1L), eq(2L),
                eq("2026-07"), eq("PRESENCA_CONFIRMADA"), any(Pageable.class));

        controller.getMedicalSlotsPaginated(model(), 0, 10, 10, 10, 1L, 2L,
                "2026-07", "CIRURGIA", "PRESENCA_CONFIRMADA");
        verify(contemplationService).findContemplationsByUBSAndSpecialty(eq(ProcedureType.CIRURGIA), eq(1L), eq(2L),
                eq("2026-07"), eq("PRESENCA_CONFIRMADA"), any(Pageable.class));

        assertThat(controller.getMedicalSlotsPaginated(model(), 0, 10, 10, 10, 1L, 2L,
                "2026-07", "OUTRO", "PRESENCA_CONFIRMADA"))
                .isEqualTo("contemplationManagement/contemplation_management");
    }

    @Test
    void deveCarregarDetalheDeContemplacao() {
        when(contemplationService.loadContemplatedById(6L)).thenReturn(contemplation);
        var model = model();

        assertThat(controller.loadContemplated(6L, 1L, 2L, "2026-07", model))
                .isEqualTo("queueManagement/queueFragments/patientAppointment_info");
        assertThat(model.get("contemplated")).isSameAs(contemplation);
        assertThat(model.get("isContemplated")).isEqualTo(true);
    }

    @Test
    void deveCancelarContemplacaoComRedirectPreservandoFiltros() {
        var loggedUser = admin();
        var redirectAttributes = redirect();

        String view = controller.cancelContemplation("motivo", 1L, 2L, "2026-07",
                6L, loggedUser, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/contemplation-management/search?basicHealthUnit=1&specialty=2&referenceMonth=2026-07");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(false);
        verify(contemplationService).cancelContemplationByAdmin(6L, "motivo", loggedUser);
    }

    @Test
    void deveRegistrarErroAoCancelarContemplacao() {
        var loggedUser = admin();
        doThrow(new CancelContemplationException("falha")).when(contemplationService)
                .cancelContemplationByAdmin(6L, "motivo", loggedUser);
        var redirectAttributes = redirect();

        String view = controller.cancelContemplation("motivo", null, null, "",
                6L, loggedUser, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/contemplation-management/search");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(true);
    }

    @Test
    void deveConfirmarContemplacaoComSucessoEErro() {
        var loggedUser = admin();
        var redirectAttributes = redirect();

        String view = controller.confirmContemplation(1L, 2L, "2026-07", 6L, loggedUser, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/contemplation-management/search?basicHealthUnit=1&specialty=2&referenceMonth=2026-07");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(false);
        verify(contemplationService).confirmContemplationByAdmin(6L, loggedUser);

        doThrow(new ConfirmContemplationException("falha")).when(contemplationService)
                .confirmContemplationByAdmin(7L, loggedUser);
        redirectAttributes = redirect();
        view = controller.confirmContemplation(null, null, "", 7L, loggedUser, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/contemplation-management/search");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(true);
    }
}
