package br.com.tecsus.sigaubs.repositories;

import br.com.tecsus.sigaubs.entities.ContemplationJobExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ContemplationJobExecutionRepository
        extends JpaRepository<ContemplationJobExecution, Long> {

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO contemplation_job_executions
                (tenant_id, execution_key, window_start, status, started_at,
                 lock_token, lease_until)
            VALUES
                (:tenantId, :executionKey, :windowStart, 'RUNNING', :startedAt,
                 :lockToken, :leaseUntil)
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("tenantId") Long tenantId,
            @Param("executionKey") String executionKey,
            @Param("windowStart") LocalDateTime windowStart,
            @Param("startedAt") LocalDateTime startedAt,
            @Param("lockToken") String lockToken,
            @Param("leaseUntil") LocalDateTime leaseUntil);

    @Modifying
    @Query("""
            UPDATE ContemplationJobExecution execution
               SET execution.status = 'RUNNING',
                   execution.startedAt = :startedAt,
                   execution.finishedAt = null,
                   execution.lockToken = :lockToken,
                   execution.leaseUntil = :leaseUntil
             WHERE execution.tenantId = :tenantId
               AND execution.executionKey = :executionKey
               AND execution.status = 'FAILED'
            """)
    int retryFailed(
            @Param("tenantId") Long tenantId,
            @Param("executionKey") String executionKey,
            @Param("startedAt") LocalDateTime startedAt,
            @Param("lockToken") String lockToken,
            @Param("leaseUntil") LocalDateTime leaseUntil);

    @Modifying
    @Query("""
            UPDATE ContemplationJobExecution execution
               SET execution.startedAt = :startedAt,
                   execution.finishedAt = null,
                   execution.lockToken = :lockToken,
                   execution.leaseUntil = :leaseUntil
             WHERE execution.tenantId = :tenantId
               AND execution.executionKey = :executionKey
               AND execution.status = 'RUNNING'
               AND execution.leaseUntil < :startedAt
            """)
    int reclaimExpired(
            @Param("tenantId") Long tenantId,
            @Param("executionKey") String executionKey,
            @Param("startedAt") LocalDateTime startedAt,
            @Param("lockToken") String lockToken,
            @Param("leaseUntil") LocalDateTime leaseUntil);

    @Modifying
    @Query("""
            UPDATE ContemplationJobExecution execution
               SET execution.leaseUntil = :leaseUntil
             WHERE execution.tenantId = :tenantId
               AND execution.executionKey = :executionKey
               AND execution.status = 'RUNNING'
               AND execution.lockToken = :lockToken
               AND execution.leaseUntil >= :renewedAt
            """)
    int renewLease(
            @Param("tenantId") Long tenantId,
            @Param("executionKey") String executionKey,
            @Param("lockToken") String lockToken,
            @Param("renewedAt") LocalDateTime renewedAt,
            @Param("leaseUntil") LocalDateTime leaseUntil);

    @Modifying
    @Query("""
            UPDATE ContemplationJobExecution execution
               SET execution.status = :status,
                   execution.finishedAt = :finishedAt
             WHERE execution.tenantId = :tenantId
               AND execution.executionKey = :executionKey
               AND execution.status = 'RUNNING'
               AND execution.lockToken = :lockToken
            """)
    int finish(
            @Param("tenantId") Long tenantId,
            @Param("executionKey") String executionKey,
            @Param("lockToken") String lockToken,
            @Param("status") String status,
            @Param("finishedAt") LocalDateTime finishedAt);

    Optional<ContemplationJobExecution>
            findFirstByTenantIdOrderByStartedAtDescIdDesc(Long tenantId);
}
