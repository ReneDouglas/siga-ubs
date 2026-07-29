package br.com.tecsus.sigaubs.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProcedureDTO {
    @NotBlank
    @Size(max = 255)
    private String description;
    @NotBlank
    @Size(max = 50)
    private String procedureType;

    public ProcedureDTO() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProcedureType() {
        return procedureType;
    }

    public void setProcedureType(String procedureType) {
        this.procedureType = procedureType;
    }
}
