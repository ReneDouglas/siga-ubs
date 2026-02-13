package br.com.tecsus.sigaubs.dtos;

public record ContemplationStatusDTO(
        String status,
        String startTime,
        String endTime,
        Long totalContemplatedToday) {
}
