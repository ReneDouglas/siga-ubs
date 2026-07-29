package br.com.tecsus.sigaubs.dtos;

import br.com.tecsus.sigaubs.utils.MedicalSlotLimits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class MedicalSlotBatchCommandDTO {

    @Valid
    @Size(max = MedicalSlotLimits.MAXIMUM_BATCH_SIZE)
    private List<MedicalSlotCommandDTO> availableMedicalSlots = new ArrayList<>();

    public List<MedicalSlotCommandDTO> getAvailableMedicalSlots() {
        return availableMedicalSlots;
    }

    public void setAvailableMedicalSlots(List<MedicalSlotCommandDTO> availableMedicalSlots) {
        this.availableMedicalSlots = availableMedicalSlots;
    }

    public void addRow(MedicalSlotCommandDTO slot) {
        availableMedicalSlots.add(slot);
    }

    public void removeRow(int index) {
        availableMedicalSlots.remove(index);
    }
}
