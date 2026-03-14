package br.com.tecsus.sigaubs.dtos;

import br.com.tecsus.sigaubs.entities.MedicalSlot;

import java.util.ArrayList;
import java.util.List;

public class AvailableMedicalSlotsFormDTO {

    private List<MedicalSlot> availableMedicalSlots = new ArrayList<>();

    public AvailableMedicalSlotsFormDTO() {
    }

    public List<MedicalSlot> getAvailableMedicalSlots() {
        return availableMedicalSlots;
    }

    public void setAvailableMedicalSlots(List<MedicalSlot> availableMedicalSlots) {
        this.availableMedicalSlots = availableMedicalSlots;
    }

    public void addRow(MedicalSlot availableMedicalSlot) {
        this.availableMedicalSlots.add(availableMedicalSlot);
    }

    public void removeRow(int index) {
        this.availableMedicalSlots.remove(index);
    }

}
