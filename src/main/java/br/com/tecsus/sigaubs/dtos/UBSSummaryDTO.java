package br.com.tecsus.sigaubs.dtos;

public record UBSSummaryDTO(
        Long id,
        String name,
        String neighborhood,
        Long totalOpenAppointments,
        Long totalContemplated,
        Long totalPatients,
        Long totalAvailableSlots,
        Long averageWaitDays) {
}
