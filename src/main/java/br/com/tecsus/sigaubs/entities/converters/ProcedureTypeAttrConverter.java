package br.com.tecsus.sigaubs.entities.converters;

import br.com.tecsus.sigaubs.enums.ProcedureType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ProcedureTypeAttrConverter implements AttributeConverter<ProcedureType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ProcedureType attribute) {

        if (attribute == null) {
            return null;
        }

        return attribute.getPersistenceCode();
    }

    @Override
    public ProcedureType convertToEntityAttribute(Integer dbData) {

        if (dbData == null) {
            return null;
        }

        return ProcedureType.fromPersistenceCode(dbData);
    }
}
