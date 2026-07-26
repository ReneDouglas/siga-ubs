package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.dtos.PatientAppointmentsHistoryDTO;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.Patient;
import br.com.tecsus.sigaubs.services.BasicHealthUnitService;
import br.com.tecsus.sigaubs.services.PatientService;
import br.com.tecsus.sigaubs.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.admin;
import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.model;
import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.sms;
import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.ubsUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientControllerTest {

    @Mock
    private PatientService patientService;
    @Mock
    private BasicHealthUnitService basicHealthUnitService;

    private BasicHealthUnit ubs;
    private PatientController controller;

    @BeforeEach
    void setUp() {
        ubs = TestDataFactory.ubs(1L, "UBS Afogados");
        controller = new PatientController(patientService, basicHealthUnitService);
    }

    @Test
    void deveAbrirPaginaDePacienteComUbsDoUsuarioOuListaParaSms() {
        when(basicHealthUnitService.findSystemUserUBS(1L)).thenReturn(ubs);
        var model = model();

        assertThat(controller.getPatientInsertPage(model, null, ubsUser(1L)))
                .isEqualTo("patientManagement/patient_management");
        assertThat(model.get("systemUserUBS")).isEqualTo(ubs);

        when(basicHealthUnitService.findAllUBS()).thenReturn(List.of(ubs));
        model = model();
        controller.getPatientInsertPage(model, null, sms());
        assertThat(model.get("basicHealthUnits")).isEqualTo(List.of(ubs));

        model = model();
        controller.getPatientInsertPage(model, null, admin());
        assertThat(model.get("basicHealthUnits")).isEqualTo(List.of(ubs));
    }

    @Test
    void deveCadastrarPacienteETratarErros() throws Exception {
        Patient patient = TestDataFactory.patient(10L, "Maria", ubs);
        var loggedUser = ubsUser(1L);
        when(basicHealthUnitService.findSystemUserUBS(1L)).thenReturn(ubs);
        when(patientService.registerPatient(patient, loggedUser))
                .thenReturn(ResultadoOperacao.sucesso(patient));
        var model = model();

        assertThat(controller.registerPatient(patient, loggedUser, model))
                .isEqualTo("patientManagement/patientFragments/patient_form");
        assertThat(model.get("error")).isEqualTo(false);
        verify(patientService).registerPatient(patient, loggedUser);

        when(patientService.registerPatient(patient, loggedUser))
                .thenThrow(new DataIntegrityViolationException("cpf"));
        model = model();
        controller.registerPatient(patient, loggedUser, model);
        assertThat(model.get("message")).isEqualTo("CPF ou Cartão SUS já cadastrados no sistema.");
        assertThat(model.get("error")).isEqualTo(true);

        var smsUser = sms();
        when(patientService.registerPatient(patient, smsUser))
                .thenThrow(new RuntimeException("falha"));
        when(basicHealthUnitService.findAllUBS()).thenReturn(List.of(ubs));
        model = model();
        controller.registerPatient(patient, smsUser, model);
        assertThat(model.get("message")).isEqualTo("Erro ao cadastrar paciente.");
        assertThat(model.get("basicHealthUnits")).isEqualTo(List.of(ubs));
    }

    @Test
    void devePrepararEditarEAtualizarPaciente() throws Exception {
        Patient patient = TestDataFactory.patient(10L, "Maria", ubs);
        var loggedUser = ubsUser(1L);
        var model = model();

        when(basicHealthUnitService.findSystemUserUBS(1L)).thenReturn(ubs);
        assertThat(controller.patientToEdit(patient, loggedUser, model))
                .isEqualTo("patientManagement/patientFragments/patient_form");
        assertThat(model.get("patient")).isSameAs(patient);
        assertThat(model.get("systemUserUBS")).isEqualTo(ubs);

        when(patientService.updatePatient(patient, loggedUser))
                .thenReturn(ResultadoOperacao.sucesso(patient));
        model = model();
        assertThat(controller.updatePatient(patient, loggedUser, model))
                .isEqualTo("patientManagement/patientFragments/patient_info");
        assertThat(model.get("patient")).isSameAs(patient);
        assertThat(model.get("error")).isEqualTo(false);

        var smsUser = sms();
        when(patientService.updatePatient(patient, smsUser))
                .thenReturn(ResultadoOperacao.falha("falha"));
        when(basicHealthUnitService.findAllUBS()).thenReturn(List.of(ubs));
        model = model();
        assertThat(controller.updatePatient(patient, smsUser, model))
                .isEqualTo("patientManagement/patientFragments/patient_form");
        assertThat(model.get("error")).isEqualTo(true);
    }

    @Test
    void deveListarPacientesComFiltroRecebidoNaPaginacao() {
        Patient filter = new Patient();
        filter.setName("Maria");
        var loggedUser = ubsUser(1L);
        var page = new PageImpl<>(List.of(TestDataFactory.patient(10L, "Maria", ubs)));
        when(patientService.findPatientsPage(eq(filter), any(PageRequest.class), eq(loggedUser))).thenReturn(page);
        var model = model();

        assertThat(controller.getPatientsPage(model, filter, loggedUser, 0, 10, false))
                .isEqualTo("patientManagement/patient_list");
        assertThat(model.get("patientsPage")).isSameAs(page);
        assertThat(model.get("patient")).isSameAs(filter);

        model = model();
        assertThat(controller.getPatientsPage(model, filter, loggedUser, 1, 10, true))
                .isEqualTo("patientManagement/patientFragments/patient_datatable");
    }

    @Test
    void deveListarHistoricoUsandoIdExplicitoNaPaginacao() {
        var page = new PageImpl<PatientAppointmentsHistoryDTO>(List.of());
        var loggedUser = ubsUser(1L);
        when(patientService.findPatientAppointmentsHistoryPage(eq(10L), any(PageRequest.class), eq(loggedUser)))
                .thenReturn(page);
        var model = model();

        assertThat(controller.getPatientAppointmentsHistory(model, loggedUser, 10L, 0, 10, false))
                .isEqualTo("patientManagement/patientFragments/patient_history");
        assertThat(model.get("patientHistoryPage")).isSameAs(page);
        assertThat(model.get("patientHistoryId")).isEqualTo(10L);

        model = model();
        assertThat(controller.getPatientAppointmentsHistory(model, loggedUser, 10L, 1, 10, true))
                .isEqualTo("patientManagement/patientFragments/patient_history");
    }

    @Test
    void devePesquisarPacienteComAutocompleteEApenasAcimaDoLimite() {
        var loggedUser = ubsUser(1L);
        var model = model();

        assertThat(controller.searchPatient("", true, loggedUser, model))
                .isEqualTo("patientManagement/patientFragments/patient_search_autocomplete");
        assertThat(model.get("patients")).isEqualTo(List.of());

        model = model();
        assertThat(controller.searchPatient("abc", false, loggedUser, model))
                .isEqualTo("patientManagement/patientFragments/patient_search_dropdown");
        assertThat(model.get("patients")).isEqualTo(List.of());

        Patient patient = TestDataFactory.patient(10L, "Maria", ubs);
        when(patientService.searchNativePatients("Maria", loggedUser)).thenReturn(List.of(patient));
        model = model();
        assertThat(controller.searchPatient("Maria", true, loggedUser, model))
                .isEqualTo("patientManagement/patientFragments/patient_search_autocomplete");
        assertThat(model.get("patients")).isEqualTo(List.of(patient));
    }

    @Test
    void deveLimparEditarEAbrirPacienteSelecionado() {
        Patient patient = TestDataFactory.patient(10L, "Maria", ubs);
        var loggedUser = ubsUser(1L);
        when(patientService.findPatientToEdit(10L, loggedUser)).thenReturn(ResultadoOperacao.sucesso(patient));

        assertThat(controller.cancelPatientEdit()).isEqualTo("redirect:/patient-management");
        assertThat(controller.clearPatientsPage()).isEqualTo("redirect:/patient-list");
        assertThat(controller.editSelectedPatient(10L)).isEqualTo("redirect:/patient-management?id=10");

        when(basicHealthUnitService.findSystemUserUBS(1L)).thenReturn(ubs);
        var model = model();
        controller.getPatientInsertPage(model, 10L, loggedUser);
        assertThat(model.get("patient")).isSameAs(patient);
    }

    @Test
    void deveExibirErroAoNaoEncontrarPacienteParaEdicao() {
        var loggedUser = ubsUser(1L);
        when(patientService.findPatientToEdit(10L, loggedUser))
                .thenReturn(ResultadoOperacao.falha("Paciente não encontrado. Contate o TI."));
        when(basicHealthUnitService.findSystemUserUBS(1L)).thenReturn(ubs);
        var model = model();

        assertThat(controller.getPatientInsertPage(model, 10L, loggedUser))
                .isEqualTo("patientManagement/patient_management");
        assertThat(model.get("patient")).isInstanceOf(Patient.class);
        assertThat(model.get("error")).isEqualTo(true);
        assertThat(model.get("message")).isEqualTo("Paciente não encontrado. Contate o TI.");
    }
}
