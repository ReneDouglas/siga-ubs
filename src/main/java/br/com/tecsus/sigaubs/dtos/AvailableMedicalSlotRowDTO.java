package br.com.tecsus.sigaubs.dtos;

import br.com.tecsus.sigaubs.entities.MedicalProcedure;
import br.com.tecsus.sigaubs.enums.ProcedureType;

public class AvailableMedicalSlotRowDTO {

    private int totalSlots;
    private ProcedureType procedureType;
    private MedicalProcedure medicalProcedure;

    public AvailableMedicalSlotRowDTO() {
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public void setTotalSlots(int totalSlots) {
        this.totalSlots = totalSlots;
    }

    public ProcedureType getProcedureType() {
        return procedureType;
    }

    public void setProcedureType(ProcedureType procedureType) {
        this.procedureType = procedureType;
    }

    public MedicalProcedure getMedicalProcedure() {
        return medicalProcedure;
    }

    public void setMedicalProcedure(MedicalProcedure medicalProcedure) {
        this.medicalProcedure = medicalProcedure;
    }
}
