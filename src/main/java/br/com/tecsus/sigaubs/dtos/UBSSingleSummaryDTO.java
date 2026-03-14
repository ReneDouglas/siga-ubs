package br.com.tecsus.sigaubs.dtos;

public record UBSSingleSummaryDTO(
        String name,
        Long totalOpenAppointments,
        Long totalContemplated,
        Long totalPatients) {
}
