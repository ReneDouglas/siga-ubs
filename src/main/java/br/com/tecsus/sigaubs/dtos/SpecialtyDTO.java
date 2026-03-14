package br.com.tecsus.sigaubs.dtos;

import java.util.ArrayList;
import java.util.List;

public class SpecialtyDTO {
    private Long id;
    private String title;
    private String description;
    private Boolean active;
    private List<ProcedureDTO> procedures = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public List<ProcedureDTO> getProcedures() {
        return procedures;
    }

    public void setProcedures(List<ProcedureDTO> procedures) {
        this.procedures = procedures;
    }
}
