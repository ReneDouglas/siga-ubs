package br.com.tecsus.sigaubs.enums;

import java.util.Arrays;
import java.util.List;

public enum ProcedureType {
    CONSULTA("Consulta"),
    EXAME("Exame"),
    CIRURGIA("Cirurgia");

    private final String description;

    ProcedureType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static ProcedureType getProcedureTypeByDescription(String d) {
        return Arrays
                .stream(values())
                .filter(type -> type.description.equals(d))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Erro ao encontrar procedimento."));
    }

    public static List<String> getMedicalProceduresDescription() {
        return Arrays.stream(values()).map(ProcedureType::getDescription).toList();
    }
}
