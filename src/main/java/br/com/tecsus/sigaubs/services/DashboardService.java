package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.*;
import br.com.tecsus.sigaubs.enums.ContemplationJobStatus;
import br.com.tecsus.sigaubs.repositories.DashboardRepository;
import br.com.tecsus.sigaubs.tenancy.TenantContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final ContemplationJobExecutionService jobExecutionService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    @Autowired
    public DashboardService(DashboardRepository dashboardRepository,
            ContemplationJobExecutionService jobExecutionService) {
        this.dashboardRepository = dashboardRepository;
        this.jobExecutionService = jobExecutionService;
    }

    public DashboardDTO loadDashboardData() {

        YearMonth currentMonth = YearMonth.now();
        LocalDate startOfMonth = currentMonth.atDay(1);
        LocalDate startOfNextMonth = currentMonth.plusMonths(1).atDay(1);

        List<UBSSummaryDTO> ubsSummaries = dashboardRepository.findAllUBSSummaries(startOfMonth, startOfNextMonth);
        List<DailyAppointmentDTO> dailyAppointments = dashboardRepository.findDailyAppointments();
        List<MonthlyStatsDTO> monthlyOpen = dashboardRepository.findMonthlyOpenAppointments();
        List<MonthlyStatsDTO> monthlyContemplations = dashboardRepository.findMonthlyContemplations();
        List<PriorityDistributionDTO> priorityDistribution = dashboardRepository.findPriorityDistribution();
        List<ProcedureTypeDistributionDTO> procedureTypeDistribution = dashboardRepository.findProcedureTypeDistribution();
        List<BottleneckDTO> topBottlenecks = dashboardRepository.findTopBottlenecks();
        List<SlotOccupancyDTO> slotOccupancy = dashboardRepository.findSlotOccupancyByUBS(startOfMonth, startOfNextMonth);

        ContemplationStatusDTO contemplationStatus = buildContemplationStatus();

        return new DashboardDTO(
                ubsSummaries,
                dailyAppointments,
                monthlyOpen,
                monthlyContemplations,
                priorityDistribution,
                contemplationStatus,
                procedureTypeDistribution,
                topBottlenecks,
                slotOccupancy);
    }

    @Transactional(readOnly = true)
    public UBSDashboardDTO loadUBSDashboardData(Long ubsId) {

        YearMonth currentMonth = YearMonth.now();
        LocalDate startOfMonth = currentMonth.atDay(1);
        LocalDate startOfNextMonth = currentMonth.plusMonths(1).atDay(1);

        UBSSingleSummaryDTO summary = dashboardRepository.findUBSSummaryByUbsId(ubsId, startOfMonth, startOfNextMonth);
        List<ContemplatedPatientRowDTO> rows = dashboardRepository
                .findContemplatedPatientsByUbsThisMonth(ubsId, startOfMonth, startOfNextMonth);

        if (summary == null) {
            return null;
        }

        return new UBSDashboardDTO(
                summary.name(),
                summary.totalOpenAppointments(),
                summary.totalContemplated(),
                summary.totalPatients(),
                rows);
    }

    private ContemplationStatusDTO buildContemplationStatus() {
        var latestExecution = jobExecutionService.findLatestForTenant(
                TenantContextHolder.getRequiredTenantId());
        Long totalToday = dashboardRepository.countTodayContemplations();
        if (latestExecution.isEmpty()) {
            return new ContemplationStatusDTO(null, null, null, totalToday);
        }

        var execution = latestExecution.get();
        ContemplationJobStatus status =
                ContemplationJobStatus.valueOf(execution.getStatus());
        String startTime = format(execution.getStartedAt());
        String endTime = format(execution.getFinishedAt());
        if (status == ContemplationJobStatus.RUNNING
                && execution.getLeaseUntil() != null
                && execution.getLeaseUntil().isBefore(
                        LocalDateTime.now(BUSINESS_ZONE))) {
            status = ContemplationJobStatus.INTERRUPTED;
            endTime = format(execution.getLeaseUntil());
        }
        return new ContemplationStatusDTO(
                status.name(), startTime, endTime, totalToday);
    }

    private String format(LocalDateTime value) {
        return value == null ? null : value.format(FORMATTER);
    }
}
