package br.com.tecsus.sigaubs.dtos;

import java.util.List;

public record UBSDashboardDTO(
        String ubsName,
        Long totalOpenAppointments,
        Long totalContemplatedThisMonth,
        Long totalPatients,
        List<ContemplatedPatientRowDTO> contemplatedThisMonth) {}
