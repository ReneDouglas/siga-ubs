package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.dtos.AvailableMedicalSlotsFormDTO;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.MedicalProcedure;
import br.com.tecsus.sigaubs.entities.MedicalSlot;
import br.com.tecsus.sigaubs.entities.Specialty;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.services.AppointmentService;
import br.com.tecsus.sigaubs.services.BasicHealthUnitService;
import br.com.tecsus.sigaubs.services.MedicalSlotService;
import br.com.tecsus.sigaubs.services.SpecialtyService;
import br.com.tecsus.sigaubs.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.model;
import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.redirect;
import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.sms;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalSlotControllerTest {

    @Mock
    private BasicHealthUnitService basicHealthUnitService;
    @Mock
    private SpecialtyService specialtyService;
    @Mock
    private MedicalSlotService medicalSlotService;
    @Mock
    private AppointmentService appointmentService;

    private BasicHealthUnit ubs;
    private Specialty specialty;
    private MedicalProcedure procedure;
    private MedicalSlotController controller;

    @BeforeEach
    void setUp() {
        ubs = TestDataFactory.ubs(1L, "UBS Afogados");
        specialty = TestDataFactory.specialty(2L, "Cardiologia");
        procedure = TestDataFactory.procedure(3L, "Consulta", ProcedureType.CONSULTA, specialty);
        controller = new MedicalSlotController(basicHealthUnitService, specialtyService, medicalSlotService,
                appointmentService);
    }

    @Test
    void deveAbrirPaginaDeVagas() {
        when(basicHealthUnitService.findAllUBS()).thenReturn(List.of(ubs));
        when(specialtyService.findSpecialties()).thenReturn(List.of(specialty));
        when(medicalSlotService.findMedicalSlotsPaginated(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        var model = model();

        assertThat(controller.getMedicalSlotPage(model))
                .isEqualTo("medicalSlotManagement/medicalSlot_management");
        assertThat(model.get("basicHealthUnits")).isEqualTo(List.of(ubs));
        assertThat(model.get("specialties")).isEqualTo(List.of(specialty));
        assertThat(model.get("medicalSlotsPage")).isInstanceOf(PageImpl.class);
    }

    @Test
    void deveAdicionarERemoverLinhaUsandoFormularioComoFonteDaVerdade() {
        MedicalSlot slot = TestDataFactory.slot(10L, TestDataFactory.ubs(1L, "Stub"), procedure, 10, 10);
        when(basicHealthUnitService.findSystemUserUBS(1L)).thenReturn(ubs);
        when(basicHealthUnitService.getFetchedAssociations(any(MedicalSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var model = model();

        assertThat(controller.addAvailableMedicalSlotsRow(slot, new AvailableMedicalSlotsFormDTO(), model))
                .isEqualTo("medicalSlotManagement/medicalSlotFragments/available_slots_form_table");
        AvailableMedicalSlotsFormDTO form = (AvailableMedicalSlotsFormDTO) model.get("availableMedicalSlotsForm");
        assertThat(form.getAvailableMedicalSlots()).hasSize(1);
        assertThat(form.getAvailableMedicalSlots().getFirst().getBasicHealthUnit()).isSameAs(ubs);

        model = model();
        assertThat(controller.removeRowtByIndex(0, form, model))
                .isEqualTo("medicalSlotManagement/medicalSlotFragments/available_slots_form_table");
        form = (AvailableMedicalSlotsFormDTO) model.get("availableMedicalSlotsForm");
        assertThat(form.getAvailableMedicalSlots()).isEmpty();
    }

    @Test
    void deveRegistrarLoteDeVagasETratarErro() throws Exception {
        var form = new AvailableMedicalSlotsFormDTO();
        form.addRow(TestDataFactory.slot(10L, ubs, procedure, 10, 10));
        var loggedUser = sms();
        var redirectAttributes = redirect();
        when(medicalSlotService.registerAvailableMedicalSlotsBatch(form, loggedUser))
                .thenReturn(ResultadoOperacao.sucessoSemValor());

        assertThat(controller.registerAvailableMedicalSlots(form, loggedUser, redirectAttributes))
                .isEqualTo("redirect:/medicalSlot-management");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(false);
        verify(medicalSlotService).registerAvailableMedicalSlotsBatch(form, loggedUser);

        when(medicalSlotService.registerAvailableMedicalSlotsBatch(form, loggedUser))
                .thenReturn(ResultadoOperacao.falha("UBS distinta"));
        redirectAttributes = redirect();
        controller.registerAvailableMedicalSlots(form, loggedUser, redirectAttributes);
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo(true);
        assertThat(redirectAttributes.getFlashAttributes().get("message")).isEqualTo("Erro ao registrar vagas: UBS distinta");
    }

    @Test
    void devePaginarECarregarProcedimentosPorTipo() {
        when(medicalSlotService.findMedicalSlotsPaginated(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        var model = model();

        assertThat(controller.getMedicalSlotsPaginated(model, 1, 5))
                .isEqualTo("medicalSlotManagement/medicalSlotFragments/medicalSlot_datatable");
        assertThat(model.get("medicalSlotsPage")).isInstanceOf(PageImpl.class);

        when(appointmentService.findBySpecialtyIdAndProcedureType(2L, ProcedureType.CONSULTA))
                .thenReturn(List.of(procedure));
        model = model();
        assertThat(controller.loadProcedure("CONSULTA", 2L, model))
                .isEqualTo("medicalSlotManagement/medicalSlotFragments/medicalProcedures");
        assertThat(model.get("isConsultation")).isEqualTo(true);
        assertThat(model.get("procedures")).isEqualTo(List.of(procedure));

        controller.loadProcedure("EXAME", 2L, model());
        verify(appointmentService).findBySpecialtyIdAndProcedureType(2L, ProcedureType.EXAME);
        controller.loadProcedure("CIRURGIA", 2L, model());
        verify(appointmentService).findBySpecialtyIdAndProcedureType(2L, ProcedureType.CIRURGIA);
    }
}
