package br.com.tecsus.sigaubs.repositories;

import br.com.tecsus.sigaubs.enums.AppointmentStatus;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.tenancy.TenantContextHolder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static br.com.tecsus.sigaubs.repositories.RepositoryMockSupport.nativeQuery;
import static br.com.tecsus.sigaubs.repositories.RepositoryMockSupport.nativeSingleResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardRepositoryTest {

    private EntityManager entityManager;
    private DashboardRepository repository;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenant(1L, "afogados");
        entityManager = mock(EntityManager.class);
        repository = new DashboardRepository();
        ReflectionTestUtils.setField(repository, "em", entityManager);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void deveMapearResumoGeralDoDashboard() {
        Query summaries = nativeQuery(List.<Object[]>of(new Object[] {1L, "UBS", "Centro", 2L, 3L, 4L, 5L, 6L}));
        Query daily = nativeQuery(List.<Object[]>of(new Object[] {"24/07", 2L}));
        Query open = nativeQuery(List.<Object[]>of(new Object[] {"Jul/2026", 3L}));
        Query contemplations = nativeQuery(List.<Object[]>of(new Object[] {"Jul/2026", 4L}));
        Query priorities = nativeQuery(List.<Object[]>of(new Object[] {2, 5L}));
        Query procedureTypes = nativeQuery(List.<Object[]>of(new Object[] {"CONSULTA", 6L}));
        Query today = nativeSingleResult(7L);
        Query bottlenecks = nativeQuery(List.<Object[]>of(new Object[] {"Cardiologia", "Consulta", 8L}));
        Query occupancy = nativeQuery(List.<Object[]>of(new Object[] {"UBS", 10L, 9L}));
        when(entityManager.createNativeQuery(anyString()))
                .thenReturn(summaries, daily, open, contemplations, priorities, procedureTypes,
                        today, bottlenecks, occupancy);

        LocalDate start = LocalDate.of(2026, 7, 1);
        LocalDate end = LocalDate.of(2026, 8, 1);

        var ubsSummaries = repository.findAllUBSSummaries(start, end);
        assertThat(ubsSummaries).hasSize(1);
        assertThat(ubsSummaries.getFirst().name()).isEqualTo("UBS");
        assertThat(ubsSummaries.getFirst().averageWaitDays()).isEqualTo(6L);

        assertThat(repository.findDailyAppointments().getFirst().day()).isEqualTo("24/07");
        assertThat(repository.findMonthlyOpenAppointments().getFirst().total()).isEqualTo(3L);
        assertThat(repository.findMonthlyContemplations().getFirst().total()).isEqualTo(4L);
        assertThat(repository.findPriorityDistribution().getFirst().total()).isEqualTo(5L);
        assertThat(repository.findProcedureTypeDistribution().getFirst().procedureType())
                .isEqualTo(ProcedureType.CONSULTA.getDescription());
        assertThat(repository.countTodayContemplations()).isEqualTo(7L);
        assertThat(repository.findTopBottlenecks().getFirst().procedure()).isEqualTo("Consulta");
        assertThat(repository.findSlotOccupancyByUBS(start, end).getFirst().consumedSlots()).isEqualTo(9L);
    }

    @Test
    void deveMapearDashboardDeUmaUbs() {
        Tuple tuple = mock(Tuple.class);
        when(tuple.get("name", String.class)).thenReturn("UBS");
        when(tuple.get("total_open")).thenReturn(2L);
        when(tuple.get("total_contemplated")).thenReturn(3L);
        when(tuple.get("total_patients")).thenReturn(4L);
        Query summary = nativeQuery(List.of(tuple));
        Query emptySummary = nativeQuery(List.of());
        Query contemplatedPatients = nativeQuery(List.<Object[]>of(new Object[] {
                "Maria", "Cardiologia", "CONSULTA", "Consulta", "Paciente Contemplado"
        }));
        when(entityManager.createNativeQuery(anyString(), eq(Tuple.class))).thenReturn(summary, emptySummary);
        when(entityManager.createNativeQuery(anyString())).thenReturn(contemplatedPatients);

        LocalDate start = LocalDate.of(2026, 7, 1);
        LocalDate end = LocalDate.of(2026, 8, 1);

        var summaryDto = repository.findUBSSummaryByUbsId(1L, start, end);
        assertThat(summaryDto.name()).isEqualTo("UBS");
        assertThat(summaryDto.totalPatients()).isEqualTo(4L);

        assertThat(repository.findUBSSummaryByUbsId(99L, start, end)).isNull();

        var patients = repository.findContemplatedPatientsByUbsThisMonth(1L, start, end);
        assertThat(patients).hasSize(1);
        assertThat(patients.getFirst().procedureType()).isEqualTo(ProcedureType.CONSULTA);
        assertThat(patients.getFirst().appointmentStatus()).isEqualTo(AppointmentStatus.PACIENTE_CONTEMPLADO);
    }
}
