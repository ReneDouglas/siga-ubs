package br.com.tecsus.sigaubs.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class TemplateUtils {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter YEARMONTH = DateTimeFormatter.ofPattern("MMM 'de' yyyy", Locale.of("pt", "BR"));

    public static String formatDate(LocalDate d) {
        return d != null ? DATE.format(d) : "";
    }

    public static String formatDateTime(LocalDateTime dt) {
        return dt != null ? DATETIME.format(dt) : "";
    }

    public static String formatYearMonth(YearMonth ym) {
        return ym != null ? YEARMONTH.format(ym) : "";
    }
}
