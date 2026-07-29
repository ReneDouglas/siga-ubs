package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.dtos.ProcedureDTO;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.dtos.SpecialtyDTO;
import br.com.tecsus.sigaubs.entities.Specialty;
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
import static br.com.tecsus.sigaubs.controllers.ControllerTestSupport.sms;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecialtyControllerTest {

    @Mock
    private SpecialtyService specialtyService;

    private Specialty specialty;
    private SpecialtyController controller;

    @BeforeEach
    void setUp() {
        specialty = TestDataFactory.specialty(1L, "Cardiologia");
        controller = new SpecialtyController(specialtyService);
    }

    @Test
    void deveAbrirPaginaECarregarEspecialidadeParaEdicao() {
        when(specialtyService.findSpecialties()).thenReturn(List.of(specialty));
        var model = model();

        assertThat(controller.getSpecialtiesPage(model)).isEqualTo("specialtyManagement/specialty_management");
        assertThat(model.get("specialties")).isEqualTo(List.of(specialty));
        assertThat(model.get("procedures")).isEqualTo(List.of());

        SpecialtyDTO dto = new SpecialtyDTO();
        dto.setId(1L);
        dto.setTitle("Cardiologia");
        ProcedureDTO procedure = new ProcedureDTO();
        procedure.setDescription("Consulta");
        procedure.setProcedureType("CONSULTA");
        dto.setProcedures(List.of(procedure));
        when(specialtyService.findFetchedSpecialty(1L)).thenReturn(dto);

        model = model();
        assertThat(controller.getSpecialtyToEdit(model, 1L)).isEqualTo("specialtyManagement/specialty_management");
        assertThat(model.get("specialtyDTO")).isSameAs(dto);
        assertThat(model.get("procedures")).isEqualTo(List.of(procedure));
    }

}
