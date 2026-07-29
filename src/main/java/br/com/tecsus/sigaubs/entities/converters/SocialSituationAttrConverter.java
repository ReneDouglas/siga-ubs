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

        return attribute.getPriority();
    }

    @Override
    public SocialSituationRating convertToEntityAttribute(Integer dbData) {

        if (dbData == null) {
            return null;
        }

        return SocialSituationRating.fromPriority(dbData);
    }
}
