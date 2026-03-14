package br.com.tecsus.sigaubs.enums;

public enum Roles {
    ROLE_ADMIN("Administrador do Sistema", false),
    ROLE_USER("Usuário", true),
    ROLE_ATENDENTE("Atendente", true),
    ROLE_ENFERMEIRO("Enfermeiro", true),
    ROLE_ACS("ACS", true),
    ROLE_SMS("Gestão da Secretaria Municipal de Saúde", true);

    private final String description;
    private final Boolean permission;

    Roles(String description, boolean permission) {
        this.description = description;
        this.permission = permission;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getPermission() {
        return permission;
    }

}
