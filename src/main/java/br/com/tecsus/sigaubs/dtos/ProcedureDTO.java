package br.com.tecsus.sigaubs.dtos;

public class ProcedureDTO {
    private String description;
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
