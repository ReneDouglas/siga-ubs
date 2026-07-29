package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.entities.ContemplationJobExecution;
import br.com.tecsus.sigaubs.enums.ContemplationJobStatus;
import br.com.tecsus.sigaubs.repositories.ContemplationJobExecutionRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContemplationJobExecutionServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 7, 29, 3, 0);
    private static final LocalDateTime LEASE_UNTIL =
            NOW.plusMinutes(30);
    private static final LocalDateTime WINDOW =
            LocalDateTime.of(2026, 7, 29, 0, 0);
    private static final String KEY = "contemplation:2026-07-29";

    private final ContemplationJobExecutionRepository repository =
            mock(ContemplationJobExecutionRepository.class);
    private final ContemplationJobExecutionService service =
            new ContemplationJobExecutionService(
                    repository,
                    Duration.ofMinutes(30),
                    Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE));

    @Test
    void deveRetomarFalhaComNovoTokenAntesDeTentarInsercao() {
        when(repository.retryFailed(
                eq(1L),
                eq(KEY),
                eq(ContemplationJobStatus.RUNNING.name()),
                eq(ContemplationJobStatus.FAILED.name()),
                eq(NOW),
                anyString(),
                eq(LEASE_UNTIL)))
                .thenReturn(1);

        var lease = service.tryStart(1L, KEY, WINDOW).orElseThrow();

        assertThat(lease.lockToken())
                .matches("[0-9a-f-]{36}");
        verify(repository, never()).reclaimExpired(
                eq(1L),
                eq(KEY),
                eq(ContemplationJobStatus.RUNNING.name()),
                eq(NOW),
                anyString(),
                eq(LEASE_UNTIL));
        verify(repository, never()).insertIfAbsent(
                eq(1L),
                eq(KEY),
                eq(WINDOW),
                eq(ContemplationJobStatus.RUNNING.name()),
                eq(NOW),
                anyString(),
                eq(LEASE_UNTIL));
    }

    @Test
    void deveRecuperarRunningComLeaseExpirada() {
        when(repository.reclaimExpired(
                eq(1L),
                eq(KEY),
                eq(ContemplationJobStatus.RUNNING.name()),
                eq(NOW),
                anyString(),
                eq(LEASE_UNTIL)))
                .thenReturn(1);

        assertThat(service.tryStart(1L, KEY, WINDOW)).isPresent();
        verify(repository, never()).insertIfAbsent(
                eq(1L),
                eq(KEY),
                eq(WINDOW),
                eq(ContemplationJobStatus.RUNNING.name()),
                eq(NOW),
                anyString(),
                eq(LEASE_UNTIL));
    }

    @Test
    void deveGarantirIdempotenciaAoInserirExecucao() {
        when(repository.insertIfAbsent(
                eq(1L),
                eq(KEY),
                eq(WINDOW),
                eq(ContemplationJobStatus.RUNNING.name()),
                eq(NOW),
                anyString(),
                eq(LEASE_UNTIL)))
                .thenReturn(0)
                .thenReturn(1);

        assertThat(service.tryStart(1L, KEY, WINDOW)).isEmpty();
        assertThat(service.tryStart(1L, KEY, WINDOW)).isPresent();
    }

    @Test
    void deveRenovarEFinalizarSomenteComTokenDaLease() {
        var lease = new ContemplationJobExecutionService.Lease(
                1L, KEY, "10000000-0000-0000-0000-000000000001");
        when(repository.renewLease(
                1L,
                KEY,
                ContemplationJobStatus.RUNNING.name(),
                lease.lockToken(),
                NOW,
                LEASE_UNTIL))
                .thenReturn(1);
        when(repository.finish(
                1L,
                KEY,
                lease.lockToken(),
                ContemplationJobStatus.COMPLETED.name(),
                ContemplationJobStatus.RUNNING.name(),
                NOW))
                .thenReturn(1);

        service.renewLease(lease);
        service.complete(lease);
        service.fail(lease);

        verify(repository).finish(
                1L,
                KEY,
                lease.lockToken(),
                ContemplationJobStatus.FAILED.name(),
                ContemplationJobStatus.RUNNING.name(),
                NOW);
        assertThat(service.newRenewalSchedule().claimIfDue()).isFalse();
    }

    @Test
    void deveInterromperWorkerQuePerdeuAPosseDaLease() {
        var staleLease = new ContemplationJobExecutionService.Lease(
                1L, KEY, "10000000-0000-0000-0000-000000000099");

        assertThatThrownBy(() -> service.renewLease(staleLease))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.complete(staleLease))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveConsultarUltimaExecucaoDoTenant() {
        ContemplationJobExecution execution = new ContemplationJobExecution();
        when(repository.findFirstByTenantIdOrderByStartedAtDescIdDesc(1L))
                .thenReturn(Optional.of(execution));

        assertThat(service.findLatestForTenant(1L)).contains(execution);
        assertThat(service.findLatestForTenant(null)).isEmpty();
    }

    @Test
    void deveRecusarDuracaoDeLeaseInsegura() {
        assertThatThrownBy(() -> new ContemplationJobExecutionService(
                repository,
                Duration.ofSeconds(30),
                Clock.system(ZONE)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ContemplationJobExecutionService(
                repository,
                Duration.ofDays(2),
                Clock.system(ZONE)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
