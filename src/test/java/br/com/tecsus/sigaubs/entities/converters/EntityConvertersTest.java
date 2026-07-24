package br.com.tecsus.sigaubs.entities.converters;

import br.com.tecsus.sigaubs.enums.AppointmentStatus;
import br.com.tecsus.sigaubs.enums.Priorities;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.enums.SocialSituationRating;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityConvertersTest {

    @Test
    void deveConverterStatusAgendamento() {
        AppointmentStatusConverter converter = new AppointmentStatusConverter();

        assertThat(converter.convertToDatabaseColumn(AppointmentStatus.PACIENTE_CONTEMPLADO))
                .isEqualTo("Paciente Contemplado");
        assertThat(converter.convertToEntityAttribute("Presença Confirmada"))
                .isEqualTo(AppointmentStatus.PRESENCA_CONFIRMADA);
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThatThrownBy(() -> converter.convertToEntityAttribute("Inexistente"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveConverterPrioridade() {
        PriorityConverter converter = new PriorityConverter();

        assertThat(converter.convertToDatabaseColumn(Priorities.URGENCIA)).isEqualTo(2);
        assertThat(converter.convertToEntityAttribute(8)).isEqualTo(Priorities.ELETIVO);
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThatThrownBy(() -> converter.convertToEntityAttribute(999))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveConverterTipoProcedimento() {
        ProcedureTypeAttrConverter converter = new ProcedureTypeAttrConverter();

        assertThat(converter.convertToDatabaseColumn(ProcedureType.CONSULTA)).isEqualTo(1);
        assertThat(converter.convertToDatabaseColumn(ProcedureType.EXAME)).isEqualTo(2);
        assertThat(converter.convertToDatabaseColumn(ProcedureType.CIRURGIA)).isEqualTo(3);
        assertThat(converter.convertToEntityAttribute(1)).isEqualTo(ProcedureType.CONSULTA);
        assertThat(converter.convertToEntityAttribute(2)).isEqualTo(ProcedureType.EXAME);
        assertThat(converter.convertToEntityAttribute(3)).isEqualTo(ProcedureType.CIRURGIA);
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThatThrownBy(() -> converter.convertToEntityAttribute(4))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveConverterSituacaoSocial() {
        SocialSituationAttrConverter converter = new SocialSituationAttrConverter();

        assertThat(converter.convertToDatabaseColumn(SocialSituationRating.UM_QUARTO_DE_SALARIO_MINIMO))
                .isEqualTo(1);
        assertThat(converter.convertToEntityAttribute(7))
                .isEqualTo(SocialSituationRating.MAIS_DE_QUATRO_SALARIOS_MINIMOS);
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThatThrownBy(() -> converter.convertToEntityAttribute(8))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveConverterYearMonthParaData() {
        YearMonthDateAttributeConverter converter = new YearMonthDateAttributeConverter();
        YearMonth reference = YearMonth.of(2026, 7);

        Date dbDate = converter.convertToDatabaseColumn(reference);

        assertThat(dbDate).isEqualTo(Date.valueOf("2026-07-01"));
        assertThat(converter.convertToEntityAttribute(dbDate)).isEqualTo(reference);
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
