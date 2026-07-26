package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO;
import br.com.tecsus.sigaubs.entities.Appointment;
import br.com.tecsus.sigaubs.entities.MedicalProcedure;
import br.com.tecsus.sigaubs.enums.AppointmentStatus;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.repositories.AppointmentRepository;
import br.com.tecsus.sigaubs.repositories.MedicalProcedureRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static br.com.tecsus.sigaubs.support.TestDataFactory.appointment;
import static br.com.tecsus.sigaubs.support.TestDataFactory.openAppointment;
import static br.com.tecsus.sigaubs.support.TestDataFactory.patient;
import static br.com.tecsus.sigaubs.support.TestDataFactory.procedure;
import static br.com.tecsus.sigaubs.support.TestDataFactory.specialty;
import static br.com.tecsus.sigaubs.support.TestDataFactory.ubs;
import static br.com.tecsus.sigaubs.support.TestDataFactory.userDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private MedicalProcedureRepository medicalProcedureRepository;

    @Mock
    private AppointmentStatusHistoryService appointmentStatusHistoryService;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void deveRegistrarMarcacaoComStatusInicialEHistorico() throws Exception {
        var loggedUser = userDetails("atendente", "Atendente", 1L, 1L, "afogados", Roles.ROLE_ATENDENTE);
        var specialty = specialty(1L, "Cardiologia");
        var procedure = procedure(10L, "Consulta", ProcedureType.CONSULTA, specialty);
        var appointment = appointment(100L, patient(1L, "Paciente", ubs(1L, "UBS")), procedure);
        when(appointmentRepository.findPatientOpenAppointments(1L)).thenReturn(List.of());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = appointmentService.registerAppointment(appointment, loggedUser);

        assertThat(resultado.sucesso()).isTrue();
        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        Appointment saved = captor.getValue();
        assertThat(saved.getContemplation()).isNull();
        assertThat(saved.getStatus()).isEqualTo(AppointmentStatus.AGUARDANDO_CONTEMPLACAO);
        assertThat(saved.getCreationUser()).isEqualTo("Atendente");
        assertThat(saved.getRequestDate()).isNotNull();
        verify(appointmentStatusHistoryService).registerAppointmentStatusHistory(saved, "Atendente");
    }

    @Test
    void deveBloquearMarcacaoDuplicadaParaMesmoProcedimento() {
        var loggedUser = userDetails("atendente", "Atendente", 1L, 1L, "afogados", Roles.ROLE_ATENDENTE);
        var procedure = procedure(10L, "Consulta", ProcedureType.CONSULTA, specialty(1L, "Cardiologia"));
        var appointment = appointment(100L, patient(1L, "Paciente", ubs(1L, "UBS")), procedure);
        PatientOpenAppointmentDTO open = openAppointment(
                200L,
                appointment.getPriority(),
                appointment.getRequestDate(),
                appointment.getPatient().getBirthDate(),
                appointment.getPatient().getSocialSituationRating(),
                appointment.getPatient().getGender());

        when(appointmentRepository.findPatientOpenAppointments(1L)).thenReturn(List.of(open));

        var resultado = appointmentService.registerAppointment(appointment, loggedUser);

        assertThat(resultado.falhou()).isTrue();
        assertThat(resultado.mensagem()).contains("consulta marcada");

        verify(appointmentRepository, never()).save(any());
        verify(appointmentStatusHistoryService, never()).registerAppointmentStatusHistory(any(), any());
    }

    @Test
    void deveCancelarMarcacaoERegistrarHistorico() throws Exception {
        var loggedUser = userDetails("atendente", "Atendente", 1L, 1L, "afogados", Roles.ROLE_ATENDENTE);
        Appointment appointment = appointment(100L,
                patient(1L, "Paciente", ubs(1L, "UBS")),
                procedure(10L, "Consulta", ProcedureType.CONSULTA, specialty(1L, "Cardiologia")));
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(appointment)).thenReturn(appointment);

        var resultado = appointmentService.cancelSolicitation(100L, loggedUser);

        assertThat(resultado.sucesso()).isTrue();
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.DESISTENCIA_PACIENTE);
        assertThat(appointment.getUpdateUser()).isEqualTo("Atendente");
        assertThat(appointment.getUpdateDate()).isNotNull();
        verify(appointmentStatusHistoryService).registerAppointmentStatusHistory(appointment, "Atendente");
    }

    @Test
    void deveDelegarConsultasParaRepository() {
        var appointment = appointment(1L, patient(1L, "Paciente", ubs(1L, "UBS")),
                procedure(1L, "Consulta", ProcedureType.CONSULTA, specialty(1L, "Cardiologia")));
        when(appointmentRepository.findByIdWithQueueDetails(1L)).thenReturn(Optional.of(appointment));

        var resultado = appointmentService.findByIdWithQueueDetails(1L);
        assertThat(resultado.sucesso()).isTrue();
        assertThat(resultado.valor()).isSameAs(appointment);

        appointmentService.findBySpecialtyIdAndProcedureType(1L, ProcedureType.CONSULTA);
        appointmentService.findPatientOpenAppointments(1L);
        appointmentService.findReferenceById(1L);

        verify(appointmentRepository).findByIdWithQueueDetails(1L);
        verify(medicalProcedureRepository).findAllBySpecialtyAndProcedureType(any(), any());
        verify(appointmentRepository).findPatientOpenAppointments(1L);
        verify(appointmentRepository).getReferenceById(1L);
    }
}
