package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.entities.AppointmentStatusHistory;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.repositories.AppointmentStatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static br.com.tecsus.sigaubs.support.TestDataFactory.appointment;
import static br.com.tecsus.sigaubs.support.TestDataFactory.patient;
import static br.com.tecsus.sigaubs.support.TestDataFactory.procedure;
import static br.com.tecsus.sigaubs.support.TestDataFactory.specialty;
import static br.com.tecsus.sigaubs.support.TestDataFactory.ubs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentStatusHistoryServiceTest {

    @Mock
    private AppointmentStatusHistoryRepository repository;

    @InjectMocks
    private AppointmentStatusHistoryService service;

    @Test
    void deveRegistrarHistoricoComStatusAtualDaMarcacao() {
        var appointment = appointment(1L, patient(1L, "Paciente", ubs(1L, "UBS")),
                procedure(10L, "Consulta", ProcedureType.CONSULTA, specialty(1L, "Cardiologia")));

        service.registerAppointmentStatusHistory(appointment, "Admin");

        ArgumentCaptor<AppointmentStatusHistory> captor = ArgumentCaptor.forClass(AppointmentStatusHistory.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAppointment()).isEqualTo(appointment);
        assertThat(captor.getValue().getStatus()).isEqualTo(appointment.getStatus());
        assertThat(captor.getValue().getCreationUser()).isEqualTo("Admin");
        assertThat(captor.getValue().getCreationDate()).isNotNull();
    }

    @Test
    void deveDelegarBuscas() {
        var appointment = appointment(1L, null, null);
        when(repository.findAllByAppointment(appointment)).thenReturn(List.of(new AppointmentStatusHistory()));

        assertThat(service.findAllAppointmentHistory(appointment)).hasSize(1);
        service.findReferenceById(1L);

        verify(repository).getReferenceById(1L);
    }
}
