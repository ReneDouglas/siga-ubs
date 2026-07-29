package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.entities.ContemplationJobExecution;
import br.com.tecsus.sigaubs.repositories.ContemplationJobExecutionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ContemplationJobExecutionService {

    private static final Duration DEFAULT_LEASE_DURATION = Duration.ofMinutes(30);
    private static final Duration MINIMUM_LEASE_DURATION = Duration.ofMinutes(1);
    private static final Duration MAXIMUM_LEASE_DURATION = Duration.ofHours(24);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    private final ContemplationJobExecutionRepository repository;
    private final Duration leaseDuration;
    private final Duration renewalInterval;
    private final Clock clock;

    @Autowired
    public ContemplationJobExecutionService(
            ContemplationJobExecutionRepository repository,
            @Value("${sigaubs.contemplation.job.lease-duration:30m}")
            Duration leaseDuration) {
        this(repository, leaseDuration, Clock.system(BUSINESS_ZONE));
    }

    ContemplationJobExecutionService(ContemplationJobExecutionRepository repository) {
        this(repository, DEFAULT_LEASE_DURATION, Clock.system(BUSINESS_ZONE));
    }

    ContemplationJobExecutionService(
            ContemplationJobExecutionRepository repository,
            Duration leaseDuration,
            Clock clock) {
        if (leaseDuration.compareTo(MINIMUM_LEASE_DURATION) < 0
                || leaseDuration.compareTo(MAXIMUM_LEASE_DURATION) > 0) {
            throw new IllegalArgumentException(
                    "A duração da lease deve estar entre 1 minuto e 24 horas.");
        }
        this.repository = repository;
        this.leaseDuration = leaseDuration;
        this.renewalInterval = leaseDuration.dividedBy(3);
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Lease> tryStart(
            Long tenantId, String executionKey, LocalDateTime windowStart) {
        LocalDateTime now = now();
        LocalDateTime leaseUntil = now.plus(leaseDuration);
        String lockToken = UUID.randomUUID().toString();

        boolean acquired = repository.retryFailed(
                tenantId, executionKey, now, lockToken, leaseUntil) == 1;
        if (!acquired) {
            acquired = repository.reclaimExpired(
                    tenantId, executionKey, now, lockToken, leaseUntil) == 1;
        }
        if (!acquired) {
            acquired = repository.insertIfAbsent(
                    tenantId,
                    executionKey,
                    windowStart,
                    now,
                    lockToken,
                    leaseUntil) == 1;
        }
        return acquired
                ? Optional.of(new Lease(tenantId, executionKey, lockToken))
                : Optional.empty();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void renewLease(Lease lease) {
        LocalDateTime renewedAt = now();
        int updated = repository.renewLease(
                lease.tenantId(),
                lease.executionKey(),
                lease.lockToken(),
                renewedAt,
                renewedAt.plus(leaseDuration));
        if (updated != 1) {
            throw new IllegalStateException(
                    "A posse da execução de contemplação foi perdida.");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(Lease lease) {
        int updated = repository.finish(
                lease.tenantId(),
                lease.executionKey(),
                lease.lockToken(),
                "COMPLETED",
                now());
        if (updated != 1) {
            throw new IllegalStateException(
                    "A execução de contemplação não pode ser concluída sem sua lease.");
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(Lease lease) {
        repository.finish(
                lease.tenantId(),
                lease.executionKey(),
                lease.lockToken(),
                "FAILED",
                now());
    }

    @Transactional(readOnly = true)
    public Optional<ContemplationJobExecution> findLatestForTenant(Long tenantId) {
        if (tenantId == null) {
            return Optional.empty();
        }
        return repository.findFirstByTenantIdOrderByStartedAtDescIdDesc(tenantId);
    }

    public RenewalSchedule newRenewalSchedule() {
        return new RenewalSchedule(renewalInterval);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public record Lease(Long tenantId, String executionKey, String lockToken) {
    }

    public static final class RenewalSchedule {

        private final long renewalIntervalNanos;
        private final AtomicLong nextRenewal;

        RenewalSchedule(Duration renewalInterval) {
            this.renewalIntervalNanos = renewalInterval.toNanos();
            this.nextRenewal = new AtomicLong(
                    System.nanoTime() + renewalIntervalNanos);
        }

        public boolean claimIfDue() {
            long now = System.nanoTime();
            long expected = nextRenewal.get();
            return now >= expected
                    && nextRenewal.compareAndSet(
                            expected, now + renewalIntervalNanos);
        }
    }
}
