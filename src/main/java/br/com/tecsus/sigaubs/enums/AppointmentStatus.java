package br.com.tecsus.sigaubs.enums;

import java.util.Arrays;
import java.util.List;

public enum AppointmentStatus {

    AGUARDANDO_CONTEMPLACAO("Aguardando Contemplação", false),
    DESISTENCIA_PACIENTE("Desistência do Paciente", false),
    PACIENTE_CONTEMPLADO("Paciente Contemplado", true),
    PRESENCA_CONFIRMADA("Presença Confirmada", true),
    CONTEMPLACAO_CANCELADA("Contemplação Cancelada", true),
    ATENDIMENTO_CONCLUIDO("Atendimento Concluído", true);

    private final String description;
    private final boolean contemplationFilter;

    AppointmentStatus(String description, boolean contemplationFilter) {
        this.description = description;
        this.contemplationFilter = contemplationFilter;
    }

    public String getDescription() {
        return description;
    }

    public boolean isContemplationFilter() {
        return contemplationFilter;
    }

    public static AppointmentStatus getByDescription(String d) {
        return Arrays
                .stream(values())
                .filter(status -> status.description.equals(d))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Erro ao encontrar status."));
    }

    public static List<AppointmentStatus> getContemplationValues() {
        return Arrays
                .stream(values())
                .filter(status -> status.contemplationFilter)
                .toList();
    }
}
