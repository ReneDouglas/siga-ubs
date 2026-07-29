package br.com.tecsus.sigaubs.enums;

public enum PatientGender {
    FEMININO("Feminino"),
    MASCULINO("Masculino");

    private final String description;

    PatientGender(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean matches(String value) {
        return description.equals(value);
    }
}
