package br.com.tecsus.sigaubs.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum ProcedureType {
    CONSULTA(1, "Consulta"),
    EXAME(2, "Exame"),
    CIRURGIA(3, "Cirurgia");

    private final int persistenceCode;
    private final String description;

    ProcedureType(int persistenceCode, String description) {
        this.persistenceCode = persistenceCode;
        this.description = description;
    }

    public int getPersistenceCode() {
        return persistenceCode;
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

    public static ProcedureType fromPersistenceCode(int persistenceCode) {
        return Arrays.stream(values())
                .filter(type -> type.persistenceCode == persistenceCode)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Tipo de procedimento não encontrado para o código: "
                                + persistenceCode));
    }

    public static List<String> getMedicalProceduresDescription() {
        return Arrays.stream(values()).map(ProcedureType::getDescription).toList();
    }
}
