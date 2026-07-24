package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.ProcedureDTO;
import br.com.tecsus.sigaubs.dtos.SpecialtyDTO;
import br.com.tecsus.sigaubs.entities.MedicalProcedure;
import br.com.tecsus.sigaubs.entities.Specialty;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.repositories.MedicalProcedureRepository;
import br.com.tecsus.sigaubs.repositories.SpecialtyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static br.com.tecsus.sigaubs.support.TestDataFactory.procedure;
import static br.com.tecsus.sigaubs.support.TestDataFactory.specialty;
import static br.com.tecsus.sigaubs.support.TestDataFactory.userDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecialtyServiceTest {

    @Mock
    private SpecialtyRepository specialtyRepository;

    @Mock
    private MedicalProcedureRepository medicalProcedureRepository;

    @InjectMocks
    private SpecialtyService service;

    @Test
    void deveConverterEspecialidadeCarregadaParaDto() {
        Specialty specialty = specialty(1L, "Cardiologia");
        specialty.setMedicalProcedures(Set.of(procedure(10L, "Consulta", ProcedureType.CONSULTA, specialty)));
        when(specialtyRepository.loadByIdWithProcedures(1L)).thenReturn(specialty);

        SpecialtyDTO dto = service.findFetchedSpecialty(1L);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTitle()).isEqualTo("Cardiologia");
        assertThat(dto.getProcedures()).singleElement().satisfies(proc -> {
            assertThat(proc.getDescription()).isEqualTo("Consulta");
            assertThat(proc.getProcedureType()).isEqualTo("Consulta");
        });
    }

    @Test
    void deveCadastrarEspecialidadeComProcedimentos() throws Exception {
        SpecialtyDTO dto = dto();

        service.registerSpecialty(dto, userDetails("admin", "Admin", null, 1L, "afogados", Roles.ROLE_SMS));

        ArgumentCaptor<Specialty> captor = ArgumentCaptor.forClass(Specialty.class);
        verify(specialtyRepository).save(captor.capture());
        Specialty saved = captor.getValue();
        assertThat(saved.getId()).isNull();
        assertThat(saved.getTitle()).isEqualTo("Cardiologia");
        assertThat(saved.getActive()).isTrue();
        assertThat(saved.getCreationUser()).isEqualTo("admin");
        assertThat(saved.getMedicalProcedures()).singleElement().satisfies(proc -> {
            assertThat(proc.getDescription()).isEqualTo("Consulta");
            assertThat(proc.getProcedureType()).isEqualTo(ProcedureType.CONSULTA);
            assertThat(proc.getSpecialty()).isEqualTo(saved);
        });
    }

    @Test
    void deveAtualizarEspecialidadeEAdicionarProcedimentos() throws Exception {
        SpecialtyDTO dto = dto();
        dto.setId(1L);
        dto.setActive(false);
        when(specialtyRepository.save(any(Specialty.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateSpecialty(dto, userDetails("admin", "Admin", null, 1L, "afogados", Roles.ROLE_SMS));

        verify(specialtyRepository).save(any(Specialty.class));
        ArgumentCaptor<MedicalProcedure> captor = ArgumentCaptor.forClass(MedicalProcedure.class);
        verify(medicalProcedureRepository).save(captor.capture());
        assertThat(captor.getValue().getSpecialty().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getCreationUser()).isEqualTo("admin");
    }

    private SpecialtyDTO dto() {
        SpecialtyDTO dto = new SpecialtyDTO();
        dto.setTitle("Cardiologia");
        dto.setDescription("Cardiologia");
        dto.setActive(true);
        ProcedureDTO procedure = new ProcedureDTO();
        procedure.setDescription("Consulta");
        procedure.setProcedureType("Consulta");
        dto.setProcedures(java.util.List.of(procedure));
        return dto;
    }
}
