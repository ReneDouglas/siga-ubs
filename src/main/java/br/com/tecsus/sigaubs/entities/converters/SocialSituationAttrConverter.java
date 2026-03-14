package br.com.tecsus.sigaubs.entities.converters;

import br.com.tecsus.sigaubs.enums.SocialSituationRating;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SocialSituationAttrConverter implements AttributeConverter<SocialSituationRating, Integer> {

    @Override
    public Integer convertToDatabaseColumn(SocialSituationRating attribute) {

        if (attribute == null) {
            return null;
        }

        return switch (attribute) {
            case UM_QUARTO_DE_SALARIO_MINIMO -> 1;
            case MEIO_SALARIO_MINIMO -> 2;
            case UM_SALARIO_MINIMO -> 3;
            case DOIS_SALARIOS_MINIMOS -> 4;
            case TRES_SALARIOS_MINIMOS -> 5;
            case QUATRO_SALARIOS_MINIMOS -> 6;
            case MAIS_DE_QUATRO_SALARIOS_MINIMOS -> 7;
        };
    }

    @Override
    public SocialSituationRating convertToEntityAttribute(Integer dbData) {

        if (dbData == null) {
            return null;
        }

        return switch (dbData) {
            case 1 -> SocialSituationRating.UM_QUARTO_DE_SALARIO_MINIMO;
            case 2 -> SocialSituationRating.MEIO_SALARIO_MINIMO;
            case 3 -> SocialSituationRating.UM_SALARIO_MINIMO;
            case 4 -> SocialSituationRating.DOIS_SALARIOS_MINIMOS;
            case 5 -> SocialSituationRating.TRES_SALARIOS_MINIMOS;
            case 6 -> SocialSituationRating.QUATRO_SALARIOS_MINIMOS;
            case 7 -> SocialSituationRating.MAIS_DE_QUATRO_SALARIOS_MINIMOS;
            default -> throw new IllegalStateException("Unexpected value: " + dbData);
        };
    }
}
