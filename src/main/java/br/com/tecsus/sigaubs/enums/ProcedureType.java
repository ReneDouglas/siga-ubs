package br.com.tecsus.sigaubs.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
        return findByDescription(d)
                .orElseThrow(() -> new IllegalArgumentException("Erro ao encontrar procedimento."));
    }

    public static Optional<ProcedureType> findByDescription(String d) {
        return Arrays
                .stream(values())
                .filter(type -> type.description.equals(d))
                .findAny();
    }

    public static List<String> getMedicalProceduresDescription() {
        return Arrays.stream(values()).map(ProcedureType::getDescription).toList();
    }
}
