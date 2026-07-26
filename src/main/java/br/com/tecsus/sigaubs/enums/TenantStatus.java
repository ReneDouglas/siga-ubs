package br.com.tecsus.sigaubs.enums;

public enum TenantStatus {
    ACTIVE("Ativo"),
    MAINTENANCE("Em manutenção"),
    DISABLED("Desabilitado");

    private final String description;

    TenantStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
