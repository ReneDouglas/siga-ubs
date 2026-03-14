package br.com.tecsus.sigaubs.enums;

public enum Priorities {
    MAIS_DE_QUATRO_MESES(1, "Mais de 4 meses", false), // Prioridade passiva
    URGENCIA(2, "Urgência", true), // Prioridade ativa
    RETORNO(3, "Retorno", true), // Prioridade ativa
    PRIORITARIO(4, "Prioritário", true), // Prioridade ativa
    //COM_HISTORICO(3, "Com histórico", true), // Prioridade ativa: É utilizado para exames e cirurgias
    IDADE(5, "Idade", false), // Prioridade passiva
    SITUACAO_SOCIAL(6, "Situação Social", false), // Prioridade passiva
    SEXO(7, "Gênero", false), // Prioridade passiva
    ELETIVO(8, "Eletivo", true), // Caso eletivo, será escolhida uma prioridade passiva
    ADMINISTRATIVO(9, "Administrativo", true), // Prioridade ativa: contemplação pela administração
    DATA_DA_MARCACAO(10, "Data da Marcação", false); // Prioridade passiva: Último campo de desempate


    // 1 é o maior nível de prioridade.
    private final int value;
    private final String description;
    private final Boolean manual;

    Priorities(int value, String description, Boolean manual) {
        this.value = value;
        this.description = description;
        this.manual = manual;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getManual() {
        return manual;
    }
}
