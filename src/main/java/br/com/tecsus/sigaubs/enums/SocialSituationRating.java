package br.com.tecsus.sigaubs.enums;

import java.util.Arrays;
import java.util.List;

public enum SocialSituationRating {
    UM_QUARTO_DE_SALARIO_MINIMO(1, "1/4 salário mínimo (R$ 353,00)"),
    MEIO_SALARIO_MINIMO(2, "1/2 salário mínimo (R$ 706,00)"),
    UM_SALARIO_MINIMO(3, "1 salário mínimo (R$ 1412,00)"),
    DOIS_SALARIOS_MINIMOS(4, "Até 2 salários mínimos (R$ 2824,00)"),
    TRES_SALARIOS_MINIMOS(5, "Até 3 salários mínimos (R$ 4236,00)"),
    QUATRO_SALARIOS_MINIMOS(6, "Até 4 salários mínimos (R$ 5648,00)"),
    MAIS_DE_QUATRO_SALARIOS_MINIMOS(7, "Mais de 4 salários mínimos");


    // 1 é o maior nível de prioridade.
    private final int priority;
    private final String description;

    SocialSituationRating(int priority, String description) {
        this.priority = priority;
        this.description = description;
    }

    public int getPriority() {
        return priority;
    }

    public String getDescription() {
        return description;
    }

    public static List<String> getDescriptionSortedByRating() {
        return Arrays.stream(values()).map(SocialSituationRating :: getDescription).toList();
    }
}
