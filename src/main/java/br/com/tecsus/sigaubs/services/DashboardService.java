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
