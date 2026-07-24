package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.entities.Appointment;
import br.com.tecsus.sigaubs.entities.Contemplation;
import br.com.tecsus.sigaubs.entities.MedicalSlot;
import br.com.tecsus.sigaubs.enums.AppointmentStatus;
import br.com.tecsus.sigaubs.enums.Priorities;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.repositories.ContemplationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static br.com.tecsus.sigaubs.support.TestDataFactory.appointment;
import static br.com.tecsus.sigaubs.support.TestDataFactory.contemplation;
import static br.com.tecsus.sigaubs.support.TestDataFactory.patient;
import static br.com.tecsus.sigaubs.support.TestDataFactory.procedure;
import static br.com.tecsus.sigaubs.support.TestDataFactory.slot;
import static br.com.tecsus.sigaubs.support.TestDataFactory.specialty;
import static br.com.tecsus.sigaubs.support.TestDataFactory.ubs;
import static br.com.tecsus.sigaubs.support.TestDataFactory.userDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContemplationServiceTest {

    @Mock
    private ContemplationRepository contemplationRepository;

    @Mock
    private MedicalSlotService medicalSlotService;

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private AppointmentStatusHistoryService appointmentStatusHistoryService;

    @InjectMocks
    private ContemplationService service;

    @Test
    void deveCarregarContemplacaoComHistorico() {
        Appointment appointment = appointment(1L, null, null);
        Contemplation contemplation = contemplation(1L, appointment, null);
        when(contemplationRepository.loadFetchedContemplationById(1L)).thenReturn(contemplation);
        when(appointmentStatusHistoryService.findAllAppointmentHistory(appointment)).thenReturn(List.of());

        Contemplation loaded = service.loadContemplatedById(1L);

        assertThat(loaded).isEqualTo(contemplation);
        verify(appointmentStatusHistoryService).findAllAppointmentHistory(appointment);
    }

    @Test
    void deveCancelarContemplacaoEDevolverVaga() throws Exception {
        var loggedUser = userDetails("admin", "Admin", null, 1L, "afogados", Roles.ROLE_SMS);
        Appointment appointment = appointment(1L, null, null);
        MedicalSlot slot = slot(1L, ubs(1L, "UBS"), null, 5, 3);
        Contemplation contemplation = contemplation(1L, appointment, slot);
        when(contemplationRepository.findFetchedForCancelById(1L)).thenReturn(contemplation);

        service.cancelContemplationByAdmin(1L, "Paciente avisou", loggedUser);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONTEMPLACAO_CANCELADA);
        assertThat(contemplation.getUpdateUser()).isEqualTo("Admin");
        assertThat(contemplation.getObservation()).contains("Cancelado por Admin").contains("Paciente avisou");
        verify(contemplationRepository).save(contemplation);
        verify(appointmentStatusHistoryService).registerAppointmentStatusHistory(appointment, "Admin");
        verify(medicalSlotService).addSlot(slot);
    }

    @Test
    void deveAnexarObservacaoAoCancelarContemplacaoComObservacaoExistente() throws Exception {
        var loggedUser = userDetails("admin", "Admin", null, 1L, "afogados", Roles.ROLE_SMS);
        Contemplation contemplation = contemplation(1L, appointment(1L, null, null), slot(1L, null, null, 5, 3));
        contemplation.setObservation("Observação anterior");
        when(contemplationRepository.findFetchedForCancelById(1L)).thenReturn(contemplation);

        service.cancelContemplationByAdmin(1L, "Motivo", loggedUser);

        assertThat(contemplation.getObservation()).startsWith("Observação anterior -- Cancelado por Admin");
    }

    @Test
    void deveConfirmarContemplacao() throws Exception {
        var loggedUser = userDetails("admin", "Admin", null, 1L, "afogados", Roles.ROLE_SMS);
        Appointment appointment = appointment(1L, null, null);
        Contemplation contemplation = contemplation(1L, appointment, null);
        when(contemplationRepository.findFetchedForCancelById(1L)).thenReturn(contemplation);

        service.confirmContemplationByAdmin(1L, loggedUser);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PRESENCA_CONFIRMADA);
        assertThat(contemplation.getObservation()).contains("Confirmado por Admin");
        verify(contemplationRepository).save(contemplation);
        verify(appointmentStatusHistoryService).registerAppointmentStatusHistory(appointment, "Admin");
    }

    @Test
    void deveContemplarMarcacaoPelaAdministracao() {
        var loggedUser = userDetails("admin", "Admin", null, 1L, "afogados", Roles.ROLE_SMS);
        var proc = procedure(10L, "Consulta", ProcedureType.CONSULTA, specialty(1L, "Cardiologia"));
        Appointment appointment = appointment(1L, patient(1L, "Paciente", ubs(1L, "UBS")), proc);
        MedicalSlot slot = slot(1L, appointment.getPatient().getBasicHealthUnit(), proc, 5, 3);
        when(appointmentService.findReferenceById(1L)).thenReturn(appointment);
        when(medicalSlotService.removeSlot(any(MedicalSlot.class))).thenReturn(slot);
        when(appointmentService.updateAppointment(appointment)).thenReturn(appointment);

        service.contemplateAppointmentByAdmin(1L, "Critério administrativo", 1L, loggedUser);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PRESENCA_CONFIRMADA);
        ArgumentCaptor<Contemplation> captor = ArgumentCaptor.forClass(Contemplation.class);
        verify(contemplationRepository).save(captor.capture());
        assertThat(captor.getValue().getContemplatedBy()).isEqualTo(Priorities.ADMINISTRATIVO);
        assertThat(captor.getValue().getMedicalSlot()).isEqualTo(slot);
        assertThat(captor.getValue().getObservation()).contains("Critério administrativo");
        verify(appointmentStatusHistoryService).registerAppointmentStatusHistory(appointment, "Admin");
    }

    @Test
    void deveDelegarBuscaPaginadaComFiltrosConvertidos() {
        service.findContemplationsByUBSAndSpecialty(
                ProcedureType.CONSULTA,
                1L,
                2L,
                "2026-07",
                "Paciente Contemplado",
                PageRequest.of(0, 10));

        verify(contemplationRepository).findConsultationsByUBSAndSpecialtyPaginated(
                any(),
                any(),
                any(),
                any(),
                any(),
                any());
    }
}
