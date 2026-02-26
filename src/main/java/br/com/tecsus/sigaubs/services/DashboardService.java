package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.*;
import br.com.tecsus.sigaubs.repositories.DashboardRepository;
import br.com.tecsus.sigaubs.utils.ContemplationScheduleStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired
    public DashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    @Transactional(readOnly = true)
    public DashboardDTO loadDashboardData() {

        List<UBSSummaryDTO> ubsSummaries = dashboardRepository.findAllUBSSummaries();
        List<DailyAppointmentDTO> dailyAppointments = dashboardRepository.findDailyAppointments();
        List<MonthlyStatsDTO> monthlyOpen = dashboardRepository.findMonthlyOpenAppointments();
        List<MonthlyStatsDTO> monthlyContemplations = dashboardRepository.findMonthlyContemplations();
        List<PriorityDistributionDTO> priorityDistribution = dashboardRepository.findPriorityDistribution();
        List<ProcedureTypeDistributionDTO> procedureTypeDistribution = dashboardRepository
                .findProcedureTypeDistribution();
        ContemplationStatusDTO contemplationStatus = buildContemplationStatus();

        return new DashboardDTO(
                ubsSummaries,
                dailyAppointments,
                monthlyOpen,
                monthlyContemplations,
                priorityDistribution,
                contemplationStatus,
                procedureTypeDistribution);
    }

    @Transactional(readOnly = true)
    public UBSDashboardDTO loadUBSDashboardData(Long ubsId) {

        Object[] summary = dashboardRepository.findUBSSummaryByUbsId(ubsId);
        List<ContemplatedPatientRowDTO> rows = dashboardRepository.findContemplatedPatientsByUbsThisMonth(ubsId);

        if (summary == null) {
            return null;
        }

        return new UBSDashboardDTO(
                (String) summary[0],
                ((Number) summary[1]).longValue(),
                ((Number) summary[2]).longValue(),
                ((Number) summary[3]).longValue(),
                rows);
    }

    private ContemplationStatusDTO buildContemplationStatus() {

        String status = ContemplationScheduleStatus.status != null
                ? ContemplationScheduleStatus.status.name()
                : null;

        String startTime = ContemplationScheduleStatus.startTime != null
                ? ContemplationScheduleStatus.startTime.format(FORMATTER)
                : null;

        String endTime = ContemplationScheduleStatus.endTime != null
                ? ContemplationScheduleStatus.endTime.format(FORMATTER)
                : null;

        Long totalToday = dashboardRepository.countTodayContemplations();

        return new ContemplationStatusDTO(status, startTime, endTime, totalToday);
    }
}
