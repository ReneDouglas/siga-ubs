package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.Patient;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.repositories.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static br.com.tecsus.sigaubs.support.TestDataFactory.patient;
import static br.com.tecsus.sigaubs.support.TestDataFactory.ubs;
import static br.com.tecsus.sigaubs.support.TestDataFactory.userDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private BasicHealthUnitService basicHealthUnitService;

    @InjectMocks
    private PatientService patientService;

    @Test
    void deveCadastrarPacienteVinculandoUbsDoUsuario() throws Exception {
        BasicHealthUnit ubs = ubs(1L, "UBS");
        Patient patient = patient(1L, "Paciente", null);
        var loggedUser = userDetails("atendente", "Atendente", 1L, 1L, "afogados", Roles.ROLE_ATENDENTE);
        when(basicHealthUnitService.findSystemUserUBSOptional(1L)).thenReturn(Optional.of(ubs));
        when(patientRepository.save(patient)).thenReturn(patient);

        var resultado = patientService.registerPatient(patient, loggedUser);

        assertThat(resultado.sucesso()).isTrue();
        Patient saved = resultado.valor();
        assertThat(saved.getBasicHealthUnit()).isEqualTo(ubs);
        assertThat(saved.getCreationUser()).isEqualTo("Atendente");
        assertThat(saved.getCreationDate()).isNotNull();
    }

    @Test
    void deveAtualizarPacienteComAuditoria() throws Exception {
        Patient patient = patient(1L, "Paciente", ubs(1L, "UBS"));
        BasicHealthUnit ubs = ubs(1L, "UBS");
        var loggedUser = userDetails("atendente", "Atendente", 1L, 1L, "afogados", Roles.ROLE_ATENDENTE);
        when(basicHealthUnitService.findSystemUserUBSOptional(1L)).thenReturn(Optional.of(ubs));
        when(patientRepository.save(patient)).thenReturn(patient);

        var resultado = patientService.updatePatient(patient, loggedUser);

        assertThat(resultado.sucesso()).isTrue();
        Patient updated = resultado.valor();
        assertThat(updated.getUpdateUser()).isEqualTo("Atendente");
        assertThat(updated.getUpdateDate()).isNotNull();
    }

    @Test
    void deveBuscarPacientePorIdEOpcionalmentePorUbs() {
        Patient patient = patient(1L, "Paciente", ubs(1L, "UBS"));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(basicHealthUnitService.findReferenceById(1L)).thenReturn(patient.getBasicHealthUnit());
        when(patientRepository.findByIdAndBasicHealthUnit(1L, patient.getBasicHealthUnit())).thenReturn(patient);

        var semFiltroUbs = patientService.findByIdAndUBS(1L, null);
        var comFiltroUbs = patientService.findByIdAndUBS(1L, 1L);
        var naoEncontrado = patientService.findByIdAndUBS(2L, null);

        assertThat(semFiltroUbs.valor()).isEqualTo(patient);
        assertThat(comFiltroUbs.valor()).isEqualTo(patient);
        assertThat(naoEncontrado.falhou()).isTrue();
        assertThat(naoEncontrado.mensagem()).isEqualTo("Paciente não encontrado.");
    }

    @Test
    void deveDelegarBuscasPaginadasComUbsDoUsuario() {
        Patient filter = new Patient();
        var loggedUser = userDetails("atendente", "Atendente", 9L, 1L, "afogados", Roles.ROLE_ATENDENTE);

        patientService.findPatientsPage(filter, PageRequest.of(0, 10), loggedUser);
        patientService.findPatientAppointmentsHistoryPage(1L, PageRequest.of(0, 10), loggedUser);
        patientService.searchNativePatients("maria", 9L);

        assertThat(filter.getBasicHealthUnit().getId()).isEqualTo(9L);
        verify(patientRepository).findPatientsPaginated(any(Patient.class), any());
        verify(patientRepository).findPatientAppointmentsHistoryPaginated(any(Patient.class), any());
        verify(patientRepository).searchNativePatientsContainingByUBS("maria", 9L);
    }
}
