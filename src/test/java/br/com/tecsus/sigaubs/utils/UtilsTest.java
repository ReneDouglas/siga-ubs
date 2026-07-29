package br.com.tecsus.sigaubs.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class UtilsTest {

    @Test
    void deveValidarAtributosNulosEVazios() {
        ValidationUtils validationUtils = new ValidationUtils();

        assertThat(validationUtils.attrIsNotNull(null)).isFalse();
        assertThat(validationUtils.attrIsNotNull("")).isFalse();
        assertThat(validationUtils.attrIsNotNull("abc")).isTrue();
        assertThat(validationUtils.attrIsNotNull(1L)).isTrue();
    }

    @Test
    void deveFormatarDatasParaTemplates() {
        assertThat(TemplateUtils.formatDate(LocalDate.of(2026, 7, 24))).isEqualTo("24/07/2026");
        assertThat(TemplateUtils.formatDateTime(LocalDateTime.of(2026, 7, 24, 18, 30, 1)))
                .isEqualTo("24/07/2026 18:30:01");
        assertThat(TemplateUtils.formatYearMonth(YearMonth.of(2026, 7))).contains("jul");
        assertThat(TemplateUtils.formatDate(null)).isEmpty();
        assertThat(TemplateUtils.formatDateTime(null)).isEmpty();
        assertThat(TemplateUtils.formatYearMonth(null)).isEmpty();
    }

    @Test
    void deveAplicarPoliticasDePaginacaoEAutocomplete() {
        assertThat(PaginationPolicy.normalizePageNumber(-1)).isZero();
        assertThat(PaginationPolicy.normalizePageSize(0)).isEqualTo(1);
        assertThat(PaginationPolicy.normalizePageSize(101)).isEqualTo(100);
        assertThat(PaginationPolicy.defaultPageRequest().getPageSize()).isEqualTo(15);

        assertThat(AutocompletePolicy.isPatientTermTooShort("abc")).isTrue();
        assertThat(AutocompletePolicy.isPatientTermTooShort("abcd")).isFalse();
        assertThat(AutocompletePolicy.isUserTermTooShort("ab")).isTrue();
        assertThat(AutocompletePolicy.isUserTermTooShort("abc")).isFalse();
    }

}
