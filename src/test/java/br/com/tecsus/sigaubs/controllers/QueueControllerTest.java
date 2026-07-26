package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.entities.Appointment;
import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.MedicalProcedure;
import br.com.tecsus.sigaubs.entities.MedicalSlot;
import br.com.tecsus.sigaubs.entities.Patient;
import br.com.tecsus.sigaubs.entities.Specialty;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.services.AppointmentService;
import br.com.tecsus.sigaubs.services.BasicHealthUnitService;
import br.com.tecsus.sigaubs.services.ContemplationService;
import br.com.tecsus.sigaubs.services.MedicalSlotService;
import br.com.tecsus.sigaubs.services.SpecialtyService;
import br.com.tecsus.sigaubs.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.admin;
import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.model;
import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.redirect;
import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.ubsUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueueControllerTest {

    @Mock
    private BasicHealthUnitService basicHealthUnitService;
    @Mock
    private SpecialtyService specialtyService;
    @Mock
    private AppointmentService appointmentService;
    @Mock
    private MedicalSlotService medicalSlotService;
    @Mock
    private ContemplationService contemplationService;

    private BasicHealthUnit ubs;
    private Specialty specialty;
    private QueueController controller;

    @BeforeEach
    void setUp() {
        ubs = TestDataFactory.ubs(1L, "UBS Afogados");
        specialty = TestDataFactory.specialty(2L, "Cardiologia");
        lenient().when(basicHealthUnitService.findAllUBS()).thenReturn(List.of(ubs));
        lenient().when(specialtyService.findSpecialties()).thenReturn(List.of(specialty));
        controller = new QueueController(basicHealthUnitService, specialtyService, appointmentService,
                medicalSlotService, contemplationService);
    }

    @Test
    void deveCarregarFilaV2ParaAdminMantendoFiltrosInformados() {
        var page = new PageImpl<PatientOpenAppointmentDTO>(List.of());
        when(appointmentService.findOpenAppointmentsQueuePaginatedV2(eq(1L), eq(2L), eq(3L), any(Pageable.class)))
                .thenReturn(page);
        var model = model();

        String view = controller.getQueuePageV2(model, 1L, 2L, 3L, "CONSULTA", admin());

        assertThat(view).isEqualTo("queueManagement/queue_management_v2");
        assertThat(model.get("basicHealthUnits")).isEqualTo(List.of(ubs));
        assertThat(model.get("specialties")).isEqualTo(List.of(specialty));
        assertThat(model.get("queuePage")).isSameAs(page);
        assertThat(model.get("selectedUBS")).isEqualTo(1L);
        assertThat(model.get("selectedSpecialty")).isEqualTo(2L);
        assertThat(model.get("selectedMedicalProcedure")).isEqualTo(3L);
        assertThat(model.get("selectedProcedureType")).isEqualTo("CONSULTA");
    }

    @Test
    void deveForcarUbsDoUsuarioComumNaFilaV2() {
        var user = ubsUser(1L);
        var page = new PageImpl<PatientOpenAppointmentDTO>(List.of());
        when(basicHealthUnitService.findSystemUserUBS(1L)).thenReturn(ubs);
        when(appointmentService.findOpenAppointmentsQueuePaginatedV2(eq(1L), eq(2L), eq(null), any(Pageable.class)))
                .thenReturn(page);
        var model = model();

        String view = controller.getQueuePageV2(model, 99L, 2L, null, null, user);

        assertThat(view).isEqualTo("queueManagement/queue_management_v2");
        assertThat(model.get("basicHealthUnits")).isEqualTo(List.of(ubs));
        assertThat(model.get("selectedUBS")).isEqualTo(1L);
        verify(appointmentService).findOpenAppointmentsQueuePaginatedV2(eq(1L), eq(2L), eq(null), any(Pageable.class));
    }

    @Test
    void devePesquisarEPaginarFilaV2RespeitandoEscopoDoUsuario() {
        var user = ubsUser(1L);
        when(appointmentService.findOpenAppointmentsQueuePaginatedV2(eq(1L), eq(2L), eq(3L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        var model = model();

        assertThat(controller.searchOpenAppointmentsQueueV2(99L, 2L, 3L, "EXAME", model, user))
                .isEqualTo("queueManagement/queueFragments/queue_datatable");
        assertThat(model.get("selectedUBS")).isEqualTo(1L);

        model = model();
        assertThat(controller.getOpenAppointmentsQueuePaginatedV2(model, 2, 5, 99L, 2L, 3L, "EXAME", user))
                .isEqualTo("queueManagement/queueFragments/queue_datatable");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(appointmentService, times(2)).findOpenAppointmentsQueuePaginatedV2(eq(1L), eq(2L), eq(3L), pageable.capture());
        assertThat(pageable.getAllValues().getLast().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getAllValues().getLast().getPageSize()).isEqualTo(5);
    }

    @Test
    void deveCarregarDetalheDaMarcacaoV2ComVagasDisponiveis() {
        MedicalProcedure procedure = TestDataFactory.procedure(3L, "Consulta", ProcedureType.CONSULTA, specialty);
        Patient patient = TestDataFactory.patient(4L, "Paciente", ubs);
        Appointment appointment = TestDataFactory.appointment(5L, patient, procedure);
        MedicalSlot slot = TestDataFactory.slot(6L, ubs, procedure, 10, 4);
        var otherAppointment = TestDataFactory.openAppointment(7L, br.com.tecsus.sigaubs.enums.Priorities.ELETIVO,
                java.time.LocalDateTime.now(), java.time.LocalDate.of(1980, 1, 1),
                br.com.tecsus.sigaubs.enums.SocialSituationRating.UM_SALARIO_MINIMO, "Feminino");
        var currentAppointment = TestDataFactory.openAppointment(5L, br.com.tecsus.sigaubs.enums.Priorities.ELETIVO,
                java.time.LocalDateTime.now(), java.time.LocalDate.of(1980, 1, 1),
                br.com.tecsus.sigaubs.enums.SocialSituationRating.UM_SALARIO_MINIMO, "Feminino");

        when(appointmentService.findById(5L)).thenReturn(appointment);
        when(appointmentService.findPatientOpenAppointments(4L))
                .thenReturn(new ArrayList<>(List.of(currentAppointment, otherAppointment)));
        when(medicalSlotService.findAvailableSlotsV2(any(MedicalSlot.class))).thenReturn(Optional.of(slot));
        var model = model();

        String view = controller.loadOpenAppointmentV2(5L, 1L, 2L, 3L, "CONSULTA", model);

        assertThat(view).isEqualTo("queueManagement/queueFragments/patientAppointment_info");
        assertThat(model.get("appointment")).isSameAs(appointment);
        assertThat(model.get("availableSlots")).isEqualTo(4);
        assertThat(model.get("medicalSlotId")).isEqualTo(6L);
        assertThat(model.get("patientOpenAppointments")).isEqualTo(List.of(otherAppointment));
    }

    @Test
    void deveContemplarPorAdminV2MontandoRedirectComFiltros() {
        var loggedUser = admin();
        var redirectAttributes = redirect();
        when(contemplationService.contemplateAppointmentByAdmin(4L, "prioridade", 5L, loggedUser))
                .thenReturn(ResultadoOperacao.sucessoSemValor());

        String view = controller.contemplateByAdminV2("prioridade", 1L, 2L, 3L, "CONSULTA",
                4L, 5L, loggedUser, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/queue-management/v2?basicHealthUnit=1&specialty=2&medicalProcedure=3&procedureType=CONSULTA");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(false);
        verify(contemplationService).contemplateAppointmentByAdmin(4L, "prioridade", 5L, loggedUser);
    }

    @Test
    void deveRegistrarErroAoContemplarPorAdminV2() {
        var loggedUser = admin();
        when(contemplationService.contemplateAppointmentByAdmin(4L, "prioridade", 5L, loggedUser))
                .thenReturn(ResultadoOperacao.falha("falha"));
        var redirectAttributes = redirect();

        String view = controller.contemplateByAdminV2("prioridade", null, null, null, "",
                4L, 5L, loggedUser, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/queue-management/v2");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(true);
    }

    @Test
    void deveCarregarProcedimentosPorTipoNaFilaV2() {
        MedicalProcedure procedure = TestDataFactory.procedure(3L, "Consulta", ProcedureType.CONSULTA, specialty);
        when(appointmentService.findBySpecialtyIdAndProcedureType(2L, ProcedureType.CONSULTA))
                .thenReturn(List.of(procedure));
        var model = model();

        assertThat(controller.loadProcedure("CONSULTA", 2L, model))
                .isEqualTo("queueManagement/queueFragments/medical_procedures_dropdown");
        assertThat(model.get("procedures")).isEqualTo(List.of(procedure));

        controller.loadProcedure("EXAME", 2L, model());
        verify(appointmentService).findBySpecialtyIdAndProcedureType(2L, ProcedureType.EXAME);

        controller.loadProcedure("CIRURGIA", 2L, model());
        verify(appointmentService).findBySpecialtyIdAndProcedureType(2L, ProcedureType.CIRURGIA);
    }

    @Test
    void deveLimparBuscaV2() {
        assertThat(controller.clearSearch()).isEqualTo("redirect:/queue-management/v2");
    }
}
