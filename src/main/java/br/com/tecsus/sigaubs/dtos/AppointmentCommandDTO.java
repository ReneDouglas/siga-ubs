package br.com.tecsus.sigaubs.dtos;

import br.com.tecsus.sigaubs.enums.Priorities;
import br.com.tecsus.sigaubs.utils.ContemplationLimits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AppointmentCommandDTO {

    @Valid
    @NotNull
    private EntityIdDTO patient = new EntityIdDTO();

    @Valid
    @NotNull
    private EntityIdDTO medicalProcedure = new EntityIdDTO();

    @NotNull
    private Priorities priority;

    @Size(max = ContemplationLimits.MAXIMUM_OBSERVATION_LENGTH)
    private String observation;

    public EntityIdDTO getPatient() {
        return patient;
    }

    public void setPatient(EntityIdDTO patient) {
        this.patient = patient;
    }

    public EntityIdDTO getMedicalProcedure() {
        return medicalProcedure;
    }

    public void setMedicalProcedure(EntityIdDTO medicalProcedure) {
        this.medicalProcedure = medicalProcedure;
    }

    public Priorities getPriority() {
        return priority;
    }

    public void setPriority(Priorities priority) {
        this.priority = priority;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }
}
