package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.entities.Appointment;
import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.MedicalProcedure;
import br.com.tecsus.sigaubs.entities.Patient;
import br.com.tecsus.sigaubs.entities.Specialty;
import br.com.tecsus.sigaubs.enums.Priorities;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.services.AppointmentService;
import br.com.tecsus.sigaubs.services.PatientService;
import br.com.tecsus.sigaubs.services.SpecialtyService;
import br.com.tecsus.sigaubs.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.model;
import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.redirect;
import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.ubsUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

    @Mock
    private PatientService patientService;
    @Mock
    private SpecialtyService specialtyService;
    @Mock
    private AppointmentService appointmentService;

    private BasicHealthUnit ubs;
    private Specialty specialty;
    private AppointmentController controller;

    @BeforeEach
    void setUp() {
        ubs = TestDataFactory.ubs(1L, "UBS Afogados");
        specialty = TestDataFactory.specialty(2L, "Cardiologia");
        controller = new AppointmentController(patientService, specialtyService, appointmentService);
    }

    @Test
    void deveAbrirPaginaDeMarcacaoComEstadoInicial() {
        when(specialtyService.findSpecialties()).thenReturn(List.of(specialty));
        var model = model();

        assertThat(controller.getAppointmentPage(model)).isEqualTo("appointmentManagement/appointment_management");
        assertThat(model.get("specialties")).isEqualTo(List.of(specialty));
        assertThat(model.get("patients")).isEqualTo(List.of());
        assertThat(model.get("loaded")).isEqualTo(false);
        assertThat(model.get("appointment")).isInstanceOf(Appointment.class);
    }

    @Test
    void deveCarregarPacienteComMarcacoesEmAberto() {
        Patient patient = TestDataFactory.patient(10L, "Maria", ubs);
        var openAppointment = TestDataFactory.openAppointment(20L, Priorities.ELETIVO,
                java.time.LocalDateTime.now(), java.time.LocalDate.of(1980, 1, 1),
                br.com.tecsus.sigaubs.enums.SocialSituationRating.UM_SALARIO_MINIMO, "Feminino");
        when(patientService.findByIdAndUBS(10L, 1L)).thenReturn(ResultadoOperacao.sucesso(patient));
        when(specialtyService.findSpecialties()).thenReturn(List.of(specialty));
        when(appointmentService.findPatientOpenAppointments(10L)).thenReturn(List.of(openAppointment));
        var model = model();

        assertThat(controller.loadPatient(10L, model, ubsUser(1L)))
                .isEqualTo("appointmentManagement/appointment_management");
        Appointment appointment = (Appointment) model.get("appointment");
        assertThat(appointment.getPatient()).isSameAs(patient);
        assertThat(model.get("patientOpenAppointments")).isEqualTo(List.of(openAppointment));
        assertThat(model.get("loaded")).isEqualTo(true);
    }

    @Test
    void deveExibirErroAoNaoEncontrarPacienteParaMarcacao() {
        when(patientService.findByIdAndUBS(10L, 1L))
                .thenReturn(ResultadoOperacao.falha("Paciente não encontrado."));
        when(specialtyService.findSpecialties()).thenReturn(List.of(specialty));
        var model = model();

        assertThat(controller.loadPatient(10L, model, ubsUser(1L)))
                .isEqualTo("appointmentManagement/appointment_management");
        assertThat(model.get("error")).isEqualTo(true);
        assertThat(model.get("message")).isEqualTo("Paciente não encontrado.");
        assertThat(model.get("loaded")).isEqualTo(false);
    }

    @Test
    void deveCarregarProcedimentosEPrioridadesPorTipo() {
        MedicalProcedure procedure = TestDataFactory.procedure(3L, "Consulta", ProcedureType.CONSULTA, specialty);
        when(appointmentService.findBySpecialtyIdAndProcedureType(2L, ProcedureType.CONSULTA))
                .thenReturn(List.of(procedure));
        var model = model();

        assertThat(controller.loadPrioritiesByProcedure("CONSULTA", 2L, false, model))
                .isEqualTo("appointmentManagement/appointmentFragments/procedure_priority_dropdown");
        assertThat(model.get("procedures")).isEqualTo(List.of(procedure));
        assertThat(model.get("priorities")).isEqualTo(List.of(Priorities.ELETIVO, Priorities.URGENCIA, Priorities.RETORNO));
        assertThat(model.get("isConsultation")).isEqualTo(true);

        controller.loadPrioritiesByProcedure("EXAME", 2L, false, model());
        verify(appointmentService).findBySpecialtyIdAndProcedureType(2L, ProcedureType.EXAME);

        controller.loadPrioritiesByProcedure("CIRURGIA", 2L, false, model());
        verify(appointmentService).findBySpecialtyIdAndProcedureType(2L, ProcedureType.CIRURGIA);
    }

    @Test
    void deveCadastrarMarcacaoETratarFalhas() throws Exception {
        Patient patient = TestDataFactory.patient(10L, "Maria", ubs);
        Appointment appointment = TestDataFactory.appointment(20L, patient,
                TestDataFactory.procedure(3L, "Consulta", ProcedureType.CONSULTA, specialty));
        var loggedUser = ubsUser(1L);
        var redirectAttributes = redirect();
        when(appointmentService.registerAppointment(appointment, loggedUser))
                .thenReturn(ResultadoOperacao.sucessoSemValor());

        assertThat(controller.registerAppointmentSolicitation(appointment, loggedUser, redirectAttributes))
                .isEqualTo("redirect:/appointment-management/load?id=10");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(false);
        verify(appointmentService).registerAppointment(appointment, loggedUser);

        when(appointmentService.registerAppointment(appointment, loggedUser))
                .thenReturn(ResultadoOperacao.falha("falha"));
        redirectAttributes = redirect();
        controller.registerAppointmentSolicitation(appointment, loggedUser, redirectAttributes);
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(true);
        assertThat(redirectAttributes.getFlashAttributes().get("message"))
                .isEqualTo("Existe uma marcação em aberto para este procedimento.");

        var smsUser = ControllerTestSupport.sms();
        when(appointmentService.registerAppointment(appointment, smsUser))
                .thenReturn(ResultadoOperacao.falha("duplicada"));
        redirectAttributes = redirect();
        controller.registerAppointmentSolicitation(appointment, smsUser, redirectAttributes);
        assertThat(redirectAttributes.getFlashAttributes().get("message"))
                .isEqualTo("Existe uma marcação em aberto para este procedimento.");
    }

    @Test
    void deveCancelarMarcacaoETratarFalha() throws Exception {
        var loggedUser = ubsUser(1L);
        var redirectAttributes = redirect();
        when(appointmentService.cancelSolicitation(20L, loggedUser))
                .thenReturn(ResultadoOperacao.sucessoSemValor());

        assertThat(controller.cancelAppointmentSolicitation(20L, 10L, loggedUser, redirectAttributes))
                .isEqualTo("redirect:/appointment-management/load?id=10");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(false);
        verify(appointmentService).cancelSolicitation(20L, loggedUser);

        when(appointmentService.cancelSolicitation(21L, loggedUser))
                .thenReturn(ResultadoOperacao.falha("falha"));
        redirectAttributes = redirect();
        controller.cancelAppointmentSolicitation(21L, 10L, loggedUser, redirectAttributes);
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(true);
    }
}
