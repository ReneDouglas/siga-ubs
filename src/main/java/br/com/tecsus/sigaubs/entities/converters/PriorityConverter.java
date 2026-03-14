package br.com.tecsus.sigaubs.entities.converters;

import br.com.tecsus.sigaubs.enums.Priorities;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Converter(autoApply = true)
public class PriorityConverter implements AttributeConverter<Priorities, Integer> {

    private static final Map<Integer, Priorities> LOOKUP = Arrays.stream(Priorities.values())
            .collect(Collectors.toUnmodifiableMap(Priorities::getValue, p -> p));

    @Override
    public Integer convertToDatabaseColumn(Priorities priority) {
        if (priority == null) {
            return null;
        }
        return priority.getValue();
    }

    @Override
    public Priorities convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        Priorities result = LOOKUP.get(dbData);
        if (result == null) {
            throw new IllegalArgumentException("Unexpected value: " + dbData);
        }
        return result;
    }
}
