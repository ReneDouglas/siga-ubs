package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.entities.Appointment;
import br.com.tecsus.sigaubs.entities.MedicalSlot;
import br.com.tecsus.sigaubs.entities.Tenant;
import br.com.tecsus.sigaubs.enums.Priorities;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.enums.SocialSituationRating;
import br.com.tecsus.sigaubs.tenancy.TenantContextHolder;
import br.com.tecsus.sigaubs.tenancy.TenantResolverService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static br.com.tecsus.sigaubs.support.TestDataFactory.appointment;
import static br.com.tecsus.sigaubs.support.TestDataFactory.openAppointment;
import static br.com.tecsus.sigaubs.support.TestDataFactory.patient;
import static br.com.tecsus.sigaubs.support.TestDataFactory.procedure;
import static br.com.tecsus.sigaubs.support.TestDataFactory.slot;
import static br.com.tecsus.sigaubs.support.TestDataFactory.specialty;
import static br.com.tecsus.sigaubs.support.TestDataFactory.tenant;
import static br.com.tecsus.sigaubs.support.TestDataFactory.ubs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContemplationScheduleServiceTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void deveEncerrarQuandoNaoHaTenantsAtivos() {
        TenantResolverService tenantResolverService = mock(TenantResolverService.class);
        MedicalSlotService medicalSlotService = mock(MedicalSlotService.class);
        when(tenantResolverService.findActiveTenants()).thenReturn(List.of());

        service(medicalSlotService, mock(AppointmentService.class), mock(ContemplationService.class),
                mock(AppointmentStatusHistoryService.class), tenantResolverService, transactionTemplate())
                .executeContemplation();

        verify(medicalSlotService, never()).findAvailableSlotsByReferenceMonth();
    }

    @Test
    void deveExecutarTenantAtivoELimparContextoMesmoSemVagas() {
        TenantResolverService tenantResolverService = mock(TenantResolverService.class);
        MedicalSlotService medicalSlotService = mock(MedicalSlotService.class);
        Tenant tenant = tenant(1L, "afogados");
        when(tenantResolverService.findActiveTenants()).thenReturn(List.of(tenant));
        when(medicalSlotService.findAvailableSlotsByReferenceMonth()).thenReturn(List.of());

        service(medicalSlotService, mock(AppointmentService.class), mock(ContemplationService.class),
                mock(AppointmentStatusHistoryService.class), tenantResolverService, transactionTemplate())
                .executeContemplation();

        verify(medicalSlotService).findAvailableSlotsByReferenceMonth();
        assertThat(TenantContextHolder.getCurrent()).isEmpty();
    }

    @Test
    void deveConcluirExecucaoComLeasePersistida() {
        TenantResolverService tenantResolverService = mock(TenantResolverService.class);
        MedicalSlotService medicalSlotService = mock(MedicalSlotService.class);
        ContemplationJobExecutionService executionService =
                mock(ContemplationJobExecutionService.class);
        var lease = new ContemplationJobExecutionService.Lease(
                1L,
                "contemplation:" + LocalDate.now(),
                "10000000-0000-0000-0000-000000000001");
        when(tenantResolverService.findActiveTenants())
                .thenReturn(List.of(tenant(1L, "afogados")));
        when(executionService.tryStart(eq(1L), any(), any()))
                .thenReturn(Optional.of(lease));
        when(executionService.newRenewalSchedule())
                .thenReturn(new ContemplationJobExecutionService.RenewalSchedule(
                        java.time.Duration.ofMinutes(10)));
        when(medicalSlotService.findAvailableSlotsByReferenceMonth())
                .thenReturn(List.of());

        new ContemplationScheduleService(
                medicalSlotService,
                mock(AppointmentService.class),
                mock(ContemplationService.class),
                mock(AppointmentStatusHistoryService.class),
                tenantResolverService,
                transactionTemplate(),
                executionService)
                .executeContemplation();

        verify(executionService).complete(lease);
        verify(executionService, never()).fail(lease);
        assertThat(TenantContextHolder.getCurrent()).isEmpty();
    }

    @Test
    void devePularTenantQuandoLeaseNaoForAdquirida() {
        TenantResolverService tenantResolverService = mock(TenantResolverService.class);
        MedicalSlotService medicalSlotService = mock(MedicalSlotService.class);
        ContemplationJobExecutionService executionService =
                mock(ContemplationJobExecutionService.class);
        when(tenantResolverService.findActiveTenants())
                .thenReturn(List.of(tenant(1L, "afogados")));
        when(executionService.tryStart(eq(1L), any(), any()))
                .thenReturn(Optional.empty());

        new ContemplationScheduleService(
                medicalSlotService,
                mock(AppointmentService.class),
                mock(ContemplationService.class),
                mock(AppointmentStatusHistoryService.class),
                tenantResolverService,
                transactionTemplate(),
                executionService)
                .executeContemplation();

        verify(medicalSlotService, never()).findAvailableSlotsByReferenceMonth();
        verify(executionService, never()).complete(any());
    }

    @Test
    void deveContinuarTentandoTenantsERelancarPrimeiraFalha() {
        TenantResolverService tenantResolverService = mock(TenantResolverService.class);
        MedicalSlotService medicalSlotService = mock(MedicalSlotService.class);
        RuntimeException failure = new RuntimeException("falha");
        when(tenantResolverService.findActiveTenants()).thenReturn(List.of(
                tenant(1L, "afogados"),
                tenant(2L, "caruaru")));
        when(medicalSlotService.findAvailableSlotsByReferenceMonth())
                .thenThrow(failure)
                .thenReturn(List.of());

        assertThatThrownBy(() -> service(medicalSlotService, mock(AppointmentService.class),
                mock(ContemplationService.class), mock(AppointmentStatusHistoryService.class),
                tenantResolverService, transactionTemplate()).executeContemplation())
                .isSameAs(failure);

        verify(medicalSlotService, times(2)).findAvailableSlotsByReferenceMonth();
        assertThat(TenantContextHolder.getCurrent()).isEmpty();
    }

    @Test
    void deveContemplarPorMaisDeQuatroMeses() {
        assertPriority(
                open(Priorities.ELETIVO, LocalDateTime.now().minusMonths(5), LocalDate.of(1990, 1, 1),
                        SocialSituationRating.UM_SALARIO_MINIMO, "Masculino"),
                open(Priorities.URGENCIA, LocalDateTime.now(), LocalDate.of(1950, 1, 1),
                        SocialSituationRating.UM_QUARTO_DE_SALARIO_MINIMO, "Feminino"),
                Priorities.MAIS_DE_QUATRO_MESES);
    }

    @Test
    void deveContemplarPorPrioridadeManual() {
        assertPriority(
                open(Priorities.URGENCIA, LocalDateTime.now(), LocalDate.of(1990, 1, 1),
                        SocialSituationRating.UM_SALARIO_MINIMO, "Masculino"),
                open(Priorities.ELETIVO, LocalDateTime.now(), LocalDate.of(1950, 1, 1),
                        SocialSituationRating.UM_QUARTO_DE_SALARIO_MINIMO, "Feminino"),
                Priorities.URGENCIA);
    }

    @Test
    void deveContemplarPorIdadeSituacaoSocialSexoOuData() {
        assertPriority(
                open(Priorities.ELETIVO, LocalDateTime.now(), LocalDate.of(1950, 1, 1),
                        SocialSituationRating.UM_SALARIO_MINIMO, "Masculino"),
                open(Priorities.ELETIVO, LocalDateTime.now(), LocalDate.of(1990, 1, 1),
                        SocialSituationRating.UM_SALARIO_MINIMO, "Masculino"),
                Priorities.IDADE);

        assertPriority(
                open(Priorities.ELETIVO, LocalDateTime.now(), LocalDate.of(1990, 1, 1),
                        SocialSituationRating.UM_QUARTO_DE_SALARIO_MINIMO, "Masculino"),
                open(Priorities.ELETIVO, LocalDateTime.now(), LocalDate.of(1990, 1, 1),
                        SocialSituationRating.MAIS_DE_QUATRO_SALARIOS_MINIMOS, "Masculino"),
                Priorities.SITUACAO_SOCIAL);

        assertPriority(
                open(Priorities.ELETIVO, LocalDateTime.now(), LocalDate.of(1990, 1, 1),
                        SocialSituationRating.UM_SALARIO_MINIMO, "Feminino"),
                open(Priorities.ELETIVO, LocalDateTime.now(), LocalDate.of(1990, 1, 1),
                        SocialSituationRating.UM_SALARIO_MINIMO, "Masculino"),
                Priorities.SEXO);

        assertPriority(
                open(Priorities.ELETIVO, LocalDateTime.now(), LocalDate.of(1990, 1, 1),
                        SocialSituationRating.UM_SALARIO_MINIMO, "Masculino"),
                open(Priorities.ELETIVO, LocalDateTime.now(), LocalDate.of(1990, 1, 1),
                        SocialSituationRating.UM_SALARIO_MINIMO, "Masculino"),
                Priorities.DATA_DA_MARCACAO);
    }

    private void assertPriority(PatientOpenAppointmentDTO current, PatientOpenAppointmentDTO next,
            Priorities expected) {
        MedicalSlotService medicalSlotService = mock(MedicalSlotService.class);
        AppointmentService appointmentService = mock(AppointmentService.class);
        ContemplationService contemplationService = mock(ContemplationService.class);
        AppointmentStatusHistoryService historyService = mock(AppointmentStatusHistoryService.class);
        TenantResolverService tenantResolverService = mock(TenantResolverService.class);

        var specialty = specialty(1L, "Cardiologia");
        var procedure = procedure(10L, "Consulta", ProcedureType.CONSULTA, specialty);
        var ubs = ubs(1L, "UBS");
        MedicalSlot slot = slot(1L, ubs, procedure, 1, 1);
        when(tenantResolverService.findActiveTenants()).thenReturn(List.of(tenant(1L, "afogados")));
        when(medicalSlotService.findAvailableSlotsByReferenceMonth()).thenReturn(List.of(slot));
        when(appointmentService.findOpenAppointmentsQueuePaginatedV2(eq(1L), eq(null), eq(10L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(current, next)));
        when(contemplationService.contemplateAppointmentByJob(
                current.appointmentId(), slot.getId(), expected))
                .thenReturn(ResultadoOperacao.sucessoSemValor());

        service(medicalSlotService, appointmentService, contemplationService, historyService,
                tenantResolverService, transactionTemplate()).executeContemplation();

        verify(contemplationService).contemplateAppointmentByJob(
                current.appointmentId(), slot.getId(), expected);
    }

    private PatientOpenAppointmentDTO open(Priorities priority, LocalDateTime requestDate, LocalDate birthDate,
            SocialSituationRating socialRating, String gender) {
        return openAppointment(100L, priority, requestDate, birthDate, socialRating, gender);
    }

    private ContemplationScheduleService service(MedicalSlotService medicalSlotService,
            AppointmentService appointmentService,
            ContemplationService contemplationService,
            AppointmentStatusHistoryService historyService,
            TenantResolverService tenantResolverService,
            TransactionTemplate transactionTemplate) {
        return new ContemplationScheduleService(medicalSlotService, appointmentService, contemplationService,
                historyService, tenantResolverService, transactionTemplate);
    }

    private TransactionTemplate transactionTemplate() {
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        doAnswer(invocation -> {
            Consumer<TransactionStatus> consumer = invocation.getArgument(0);
            consumer.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        return transactionTemplate;
    }
}
