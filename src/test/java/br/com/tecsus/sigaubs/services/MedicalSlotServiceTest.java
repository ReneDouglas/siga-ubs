package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.AvailableMedicalSlotsFormDTO;
import br.com.tecsus.sigaubs.entities.MedicalSlot;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.repositories.MedicalSlotRepository;
import br.com.tecsus.sigaubs.services.exceptions.DistinctAvailableMedicalSlotException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static br.com.tecsus.sigaubs.support.TestDataFactory.procedure;
import static br.com.tecsus.sigaubs.support.TestDataFactory.slot;
import static br.com.tecsus.sigaubs.support.TestDataFactory.specialty;
import static br.com.tecsus.sigaubs.support.TestDataFactory.ubs;
import static br.com.tecsus.sigaubs.support.TestDataFactory.userDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalSlotServiceTest {

    @Mock
    private MedicalSlotRepository medicalSlotRepository;

    @InjectMocks
    private MedicalSlotService medicalSlotService;

    @Test
    void deveRegistrarLoteDeVagasParaMesmaUbs() throws Exception {
        var ubs = ubs(1L, "UBS");
        var procedure = procedure(10L, "Consulta", ProcedureType.CONSULTA, specialty(1L, "Cardiologia"));
        AvailableMedicalSlotsFormDTO form = new AvailableMedicalSlotsFormDTO();
        form.setAvailableMedicalSlots(List.of(
                slot(1L, ubs, procedure, 5, 0),
                slot(2L, ubs, procedure, 3, 0)));

        medicalSlotService.registerAvailableMedicalSlotsBatch(
                form,
                userDetails("admin", "Admin", null, 1L, "afogados", Roles.ROLE_SMS));

        ArgumentCaptor<List<MedicalSlot>> captor = ArgumentCaptor.forClass(List.class);
        verify(medicalSlotRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(MedicalSlot::getCurrentSlots).containsExactly(5, 3);
        assertThat(captor.getValue()).extracting(MedicalSlot::getCreationUser).containsExactly("Admin", "Admin");
        assertThat(captor.getValue()).allMatch(slot -> slot.getCreationDate() != null);
    }

    @Test
    void deveBloquearLoteComUbsDistintas() {
        var procedure = procedure(10L, "Consulta", ProcedureType.CONSULTA, specialty(1L, "Cardiologia"));
        AvailableMedicalSlotsFormDTO form = new AvailableMedicalSlotsFormDTO();
        form.setAvailableMedicalSlots(List.of(
                slot(1L, ubs(1L, "UBS 1"), procedure, 5, 0),
                slot(2L, ubs(2L, "UBS 2"), procedure, 3, 0)));

        assertThatThrownBy(() -> medicalSlotService.registerAvailableMedicalSlotsBatch(
                form,
                userDetails("admin", "Admin", null, 1L, "afogados", Roles.ROLE_SMS)))
                .isInstanceOf(DistinctAvailableMedicalSlotException.class);

        verify(medicalSlotRepository, never()).saveAll(any());
    }

    @Test
    void deveAdicionarRemoverVagaERespeitarLimites() {
        var stored = slot(1L, ubs(1L, "UBS"), null, 5, 4);
        when(medicalSlotRepository.getReferenceById(1L)).thenReturn(stored);
        when(medicalSlotRepository.saveAndFlush(stored)).thenReturn(stored);

        assertThat(medicalSlotService.addSlot(stored).getCurrentSlots()).isEqualTo(5);
        assertThatThrownBy(() -> medicalSlotService.addSlot(stored))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("limite máximo");

        stored.setCurrentSlots(1);
        assertThat(medicalSlotService.removeSlot(stored).getCurrentSlots()).isZero();
        assertThatThrownBy(() -> medicalSlotService.removeSlot(stored))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Não há mais slots");
    }

    @Test
    void deveDelegarBuscasParaRepository() {
        var medicalSlot = slot(1L, ubs(1L, "UBS"),
                procedure(10L, "Consulta", ProcedureType.CONSULTA, specialty(1L, "Cardiologia")),
                5,
                3);

        medicalSlotService.findAvailableSlots(medicalSlot);
        medicalSlotService.findAvailableSlotsV2(medicalSlot);
        medicalSlotService.findAvailableSlotsByReferenceMonth();

        verify(medicalSlotRepository).findByMedicalProcedureAndBasicHealthUnitAndContemplationsIsNull(
                medicalSlot.getMedicalProcedure(), medicalSlot.getBasicHealthUnit());
        verify(medicalSlotRepository).findAvailableSlotsByMedicalProcedureAndUBS(10L, 1L);
        verify(medicalSlotRepository).findAllAvailableSlotsByReferenceMonth(any(), any());
    }
}
