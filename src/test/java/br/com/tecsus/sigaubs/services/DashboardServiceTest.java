package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.UBSSingleSummaryDTO;
import br.com.tecsus.sigaubs.repositories.DashboardRepository;
import br.com.tecsus.sigaubs.utils.ContemplationScheduleStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private ContemplationScheduleStatus contemplationScheduleStatus;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void deveMontarDashboardAdminComStatusDaRotina() {
        when(contemplationScheduleStatus.getStatus()).thenReturn(ContemplationScheduleStatus.Status.RUNNING);
        when(contemplationScheduleStatus.getStartTime()).thenReturn(LocalDateTime.of(2026, 7, 24, 18, 0));
        when(dashboardRepository.countTodayContemplations()).thenReturn(3L);

        var dashboard = dashboardService.loadDashboardData();

        assertThat(dashboard.contemplationStatus().status()).isEqualTo("RUNNING");
        assertThat(dashboard.contemplationStatus().startTime()).isEqualTo("24/07/2026 18:00");
        assertThat(dashboard.contemplationStatus().totalContemplatedToday()).isEqualTo(3L);
        verify(dashboardRepository).findAllUBSSummaries(any(), any());
        verify(dashboardRepository).findDailyAppointments();
        verify(dashboardRepository).findMonthlyOpenAppointments();
        verify(dashboardRepository).findMonthlyContemplations();
        verify(dashboardRepository).findPriorityDistribution();
        verify(dashboardRepository).findProcedureTypeDistribution();
        verify(dashboardRepository).findTopBottlenecks();
        verify(dashboardRepository).findSlotOccupancyByUBS(any(), any());
    }

    @Test
    void deveMontarDashboardDaUbsOuRetornarNull() {
        when(dashboardRepository.findUBSSummaryByUbsId(any(), any(), any()))
                .thenReturn(new UBSSingleSummaryDTO("UBS", 10L, 2L, 30L))
                .thenReturn(null);
        when(dashboardRepository.findContemplatedPatientsByUbsThisMonth(any(), any(), any()))
                .thenReturn(List.of());

        var dashboard = dashboardService.loadUBSDashboardData(1L);

        assertThat(dashboard.ubsName()).isEqualTo("UBS");
        assertThat(dashboard.totalOpenAppointments()).isEqualTo(10L);
        assertThat(dashboardService.loadUBSDashboardData(1L)).isNull();
    }
}
