package br.com.tecsus.sigaubs.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.YearMonth;

public class MedicalSlotCommandDTO {

    @NotNull
    private YearMonth referenceMonth;

    @NotNull
    @Min(1)
    @Max(10_000)
    private Integer totalSlots;

    @Valid
    @NotNull
    private EntityIdDTO medicalProcedure = new EntityIdDTO();

    @Valid
    @NotNull
    private EntityIdDTO basicHealthUnit = new EntityIdDTO();

    private String specialtyTitle;
    private String procedureTypeDescription;
    private String procedureDescription;
    private String basicHealthUnitName;

    public YearMonth getReferenceMonth() {
        return referenceMonth;
    }

    public void setReferenceMonth(YearMonth referenceMonth) {
        this.referenceMonth = referenceMonth;
    }

    public Integer getTotalSlots() {
        return totalSlots;
    }

    public void setTotalSlots(Integer totalSlots) {
        this.totalSlots = totalSlots;
    }

    public EntityIdDTO getMedicalProcedure() {
        return medicalProcedure;
    }

    public void setMedicalProcedure(EntityIdDTO medicalProcedure) {
        this.medicalProcedure = medicalProcedure;
    }

    public EntityIdDTO getBasicHealthUnit() {
        return basicHealthUnit;
    }

    public void setBasicHealthUnit(EntityIdDTO basicHealthUnit) {
        this.basicHealthUnit = basicHealthUnit;
    }

    public String getSpecialtyTitle() {
        return specialtyTitle;
    }

    public void setSpecialtyTitle(String specialtyTitle) {
        this.specialtyTitle = specialtyTitle;
    }

    public String getProcedureTypeDescription() {
        return procedureTypeDescription;
    }

    public void setProcedureTypeDescription(String procedureTypeDescription) {
        this.procedureTypeDescription = procedureTypeDescription;
    }

    public String getProcedureDescription() {
        return procedureDescription;
    }

    public void setProcedureDescription(String procedureDescription) {
        this.procedureDescription = procedureDescription;
    }

    public String getBasicHealthUnitName() {
        return basicHealthUnitName;
    }

    public void setBasicHealthUnitName(String basicHealthUnitName) {
        this.basicHealthUnitName = basicHealthUnitName;
    }
}
