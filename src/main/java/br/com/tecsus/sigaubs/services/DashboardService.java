package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.*;
import br.com.tecsus.sigaubs.repositories.DashboardRepository;
import br.com.tecsus.sigaubs.utils.ContemplationScheduleStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final ContemplationScheduleStatus contemplationScheduleStatus;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired
    public DashboardService(DashboardRepository dashboardRepository,
            ContemplationScheduleStatus contemplationScheduleStatus) {
        this.dashboardRepository = dashboardRepository;
        this.contemplationScheduleStatus = contemplationScheduleStatus;
    }

    public DashboardDTO loadDashboardData() {

        YearMonth currentMonth = YearMonth.now();
        LocalDate startOfMonth = currentMonth.atDay(1);
        LocalDate startOfNextMonth = currentMonth.plusMonths(1).atDay(1);

        CompletableFuture<List<UBSSummaryDTO>> ubsSummariesFuture = CompletableFuture
                .supplyAsync(() -> dashboardRepository.findAllUBSSummaries(startOfMonth, startOfNextMonth));

        CompletableFuture<List<DailyAppointmentDTO>> dailyAppointmentsFuture = CompletableFuture
                .supplyAsync(dashboardRepository::findDailyAppointments);

        CompletableFuture<List<MonthlyStatsDTO>> monthlyOpenFuture = CompletableFuture
                .supplyAsync(dashboardRepository::findMonthlyOpenAppointments);

        CompletableFuture<List<MonthlyStatsDTO>> monthlyContemplationsFuture = CompletableFuture
                .supplyAsync(dashboardRepository::findMonthlyContemplations);

        CompletableFuture<List<PriorityDistributionDTO>> priorityDistributionFuture = CompletableFuture
                .supplyAsync(dashboardRepository::findPriorityDistribution);

        CompletableFuture<List<ProcedureTypeDistributionDTO>> procedureTypeDistributionFuture = CompletableFuture
                .supplyAsync(dashboardRepository::findProcedureTypeDistribution);

        CompletableFuture<List<BottleneckDTO>> topBottlenecksFuture = CompletableFuture
                .supplyAsync(dashboardRepository::findTopBottlenecks);

        CompletableFuture<List<SlotOccupancyDTO>> slotOccupancyFuture = CompletableFuture
                .supplyAsync(() -> dashboardRepository.findSlotOccupancyByUBS(startOfMonth, startOfNextMonth));

        CompletableFuture.allOf(
                ubsSummariesFuture, dailyAppointmentsFuture, monthlyOpenFuture,
                monthlyContemplationsFuture, priorityDistributionFuture,
                procedureTypeDistributionFuture, topBottlenecksFuture, slotOccupancyFuture
        ).join();

        ContemplationStatusDTO contemplationStatus = buildContemplationStatus();

        return new DashboardDTO(
                ubsSummariesFuture.join(),
                dailyAppointmentsFuture.join(),
                monthlyOpenFuture.join(),
                monthlyContemplationsFuture.join(),
                priorityDistributionFuture.join(),
                contemplationStatus,
                procedureTypeDistributionFuture.join(),
                topBottlenecksFuture.join(),
                slotOccupancyFuture.join());
    }

    @Transactional(readOnly = true)
    public UBSDashboardDTO loadUBSDashboardData(Long ubsId) {

        YearMonth currentMonth = YearMonth.now();
        LocalDate startOfMonth = currentMonth.atDay(1);
        LocalDate startOfNextMonth = currentMonth.plusMonths(1).atDay(1);

        Object[] summary = dashboardRepository.findUBSSummaryByUbsId(ubsId, startOfMonth, startOfNextMonth);
        List<ContemplatedPatientRowDTO> rows = dashboardRepository
                .findContemplatedPatientsByUbsThisMonth(ubsId, startOfMonth, startOfNextMonth);

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

        ContemplationScheduleStatus.Status currentStatus = contemplationScheduleStatus.getStatus();
        String status = currentStatus != null ? currentStatus.name() : null;

        String startTime = contemplationScheduleStatus.getStartTime() != null
                ? contemplationScheduleStatus.getStartTime().format(FORMATTER)
                : null;

        String endTime = contemplationScheduleStatus.getEndTime() != null
                ? contemplationScheduleStatus.getEndTime().format(FORMATTER)
                : null;

        Long totalToday = dashboardRepository.countTodayContemplations();

        return new ContemplationStatusDTO(status, startTime, endTime, totalToday);
    }
}
