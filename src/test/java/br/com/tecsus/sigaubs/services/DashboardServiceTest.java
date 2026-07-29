package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.UBSSingleSummaryDTO;
import br.com.tecsus.sigaubs.entities.ContemplationJobExecution;
import br.com.tecsus.sigaubs.repositories.DashboardRepository;
import br.com.tecsus.sigaubs.tenancy.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private ContemplationJobExecutionService jobExecutionService;

    @InjectMocks
    private DashboardService dashboardService;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void deveMontarDashboardComStatusPersistidoDoTenant() {
        TenantContextHolder.setTenant(1L, "afogados");
        ContemplationJobExecution execution = execution(
                "COMPLETED",
                LocalDateTime.of(2026, 7, 24, 18, 0),
                LocalDateTime.of(2026, 7, 24, 18, 15),
                LocalDateTime.of(2026, 7, 24, 18, 30));
        when(jobExecutionService.findLatestForTenant(1L))
                .thenReturn(Optional.of(execution));
        when(dashboardRepository.countTodayContemplations()).thenReturn(3L);

        var dashboard = dashboardService.loadDashboardData();

        assertThat(dashboard.contemplationStatus().status()).isEqualTo("COMPLETED");
        assertThat(dashboard.contemplationStatus().startTime()).isEqualTo("24/07/2026 18:00");
        assertThat(dashboard.contemplationStatus().endTime()).isEqualTo("24/07/2026 18:15");
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
    void deveSinalizarRunningComLeaseExpiradaComoInterrompido() {
        TenantContextHolder.setTenant(2L, "caruaru");
        LocalDateTime expiredAt = LocalDateTime.now(
                ZoneId.of("America/Sao_Paulo")).minusMinutes(1);
        when(jobExecutionService.findLatestForTenant(2L))
                .thenReturn(Optional.of(execution(
                        "RUNNING",
                        expiredAt.minusMinutes(30),
                        null,
                        expiredAt)));
        when(dashboardRepository.countTodayContemplations()).thenReturn(0L);

        var status = dashboardService.loadDashboardData().contemplationStatus();

        assertThat(status.status()).isEqualTo("INTERRUPTED");
        assertThat(status.endTime()).isEqualTo(
                expiredAt.format(java.time.format.DateTimeFormatter
                        .ofPattern("dd/MM/yyyy HH:mm")));
    }

    @Test
    void deveExibirAguardandoQuandoTenantNaoPossuiExecucao() {
        TenantContextHolder.setTenant(1L, "afogados");
        when(jobExecutionService.findLatestForTenant(1L))
                .thenReturn(Optional.empty());
        when(dashboardRepository.countTodayContemplations()).thenReturn(0L);

        var status = dashboardService.loadDashboardData().contemplationStatus();

        assertThat(status.status()).isNull();
        assertThat(status.startTime()).isNull();
        assertThat(status.endTime()).isNull();
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

    private ContemplationJobExecution execution(
            String status,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            LocalDateTime leaseUntil) {
        ContemplationJobExecution execution = new ContemplationJobExecution();
        execution.setStatus(status);
        execution.setStartedAt(startedAt);
        execution.setFinishedAt(finishedAt);
        execution.setLeaseUntil(leaseUntil);
        return execution;
    }
}
