package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.AvailableMedicalSlotsFormDTO;
import br.com.tecsus.sigaubs.entities.MedicalSlot;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.repositories.MedicalSlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static br.com.tecsus.sigaubs.support.TestDataFactory.procedure;
import static br.com.tecsus.sigaubs.support.TestDataFactory.slot;
import static br.com.tecsus.sigaubs.support.TestDataFactory.specialty;
import static br.com.tecsus.sigaubs.support.TestDataFactory.ubs;
import static br.com.tecsus.sigaubs.support.TestDataFactory.userDetails;
import static org.assertj.core.api.Assertions.assertThat;
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

        var resultado = medicalSlotService.registerAvailableMedicalSlotsBatch(
                form,
                userDetails("admin", "Admin", null, 1L, "afogados", Roles.ROLE_SMS));

        assertThat(resultado.sucesso()).isTrue();
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

        var resultado = medicalSlotService.registerAvailableMedicalSlotsBatch(
                form,
                userDetails("admin", "Admin", null, 1L, "afogados", Roles.ROLE_SMS));

        assertThat(resultado.falhou()).isTrue();
        assertThat(resultado.mensagem()).contains("UBS");

        verify(medicalSlotRepository, never()).saveAll(any());
    }

    @Test
    void deveAdicionarRemoverVagaERespeitarLimites() {
        var stored = slot(1L, ubs(1L, "UBS"), null, 5, 4);
        when(medicalSlotRepository.incrementIfBelowTotal(1L)).thenAnswer(invocation -> {
            if (stored.getCurrentSlots() >= stored.getTotalSlots()) {
                return 0;
            }
            stored.setCurrentSlots(stored.getCurrentSlots() + 1);
            return 1;
        });
        when(medicalSlotRepository.decrementIfAvailable(1L)).thenAnswer(invocation -> {
            if (stored.getCurrentSlots() <= 0) {
                return 0;
            }
            stored.setCurrentSlots(stored.getCurrentSlots() - 1);
            return 1;
        });
        when(medicalSlotRepository.findById(1L)).thenReturn(Optional.of(stored));

        var addResult = medicalSlotService.addSlot(stored);
        assertThat(addResult.sucesso()).isTrue();
        assertThat(addResult.valor().getCurrentSlots()).isEqualTo(5);

        var addFailure = medicalSlotService.addSlot(stored);
        assertThat(addFailure.falhou()).isTrue();
        assertThat(addFailure.mensagem()).contains("limite máximo");

        stored.setCurrentSlots(1);
        var removeResult = medicalSlotService.removeSlot(stored);
        assertThat(removeResult.sucesso()).isTrue();
        assertThat(removeResult.valor().getCurrentSlots()).isZero();

        var removeFailure = medicalSlotService.removeSlot(stored);
        assertThat(removeFailure.falhou()).isTrue();
        assertThat(removeFailure.mensagem()).contains("Não há mais slots");
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
