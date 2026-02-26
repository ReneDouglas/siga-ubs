package br.com.tecsus.sigaubs.dtos;

import java.util.List;

public record DashboardDTO(
                List<UBSSummaryDTO> ubsSummaries,
                List<DailyAppointmentDTO> dailyAppointments,
                List<MonthlyStatsDTO> monthlyOpenAppointments,
                List<MonthlyStatsDTO> monthlyContemplations,
                List<PriorityDistributionDTO> priorityDistribution,
                ContemplationStatusDTO contemplationStatus,
                List<ProcedureTypeDistributionDTO> procedureTypeDistribution) {
}
