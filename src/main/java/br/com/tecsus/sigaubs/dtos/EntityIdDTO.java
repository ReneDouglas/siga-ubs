package br.com.tecsus.sigaubs.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class EntityIdDTO {

    @NotNull
    @Positive
    private Long id;

    public EntityIdDTO() {
    }

    public EntityIdDTO(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
