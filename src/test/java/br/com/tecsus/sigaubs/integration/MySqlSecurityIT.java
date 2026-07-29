package br.com.tecsus.sigaubs.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MySqlSecurityIT {

    private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.0.43")
            .asCompatibleSubstituteFor("mysql");

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(MYSQL_IMAGE)
            .withDatabaseName("sigaubs")
            .withUsername("sigaubs_test")
            .withPassword("security_test_only")
            .withFileSystemBind(
                    absolute("docker/mysql/01-schema.sql"),
                    "/docker-entrypoint-initdb.d/01-schema.sql",
                    BindMode.READ_ONLY)
            .withFileSystemBind(
                    absolute("docker/mysql/02-seed.sql"),
                    "/docker-entrypoint-initdb.d/02-seed.sql",
                    BindMode.READ_ONLY)
            .withFileSystemBind(
                    absolute("docker/mysql/my.cnf"),
                    "/etc/mysql/conf.d/sigaubs.cnf",
                    BindMode.READ_ONLY);

    @Test
    @Order(1)
    void ddlESeedCarregamSemDivergenciaERejeitamReferenciaCrossTenant() throws Exception {
        try (Connection connection = connection()) {
            assertThat(singleLong(connection, "SELECT COUNT(*) FROM patients")).isPositive();
            assertThat(singleLong(connection, """
                    SELECT
                      (SELECT COUNT(*) FROM patients p
                        JOIN basic_health_units b ON b.id=p.id_basic_health_unit
                       WHERE p.tenant_id<>b.tenant_id)
                    + (SELECT COUNT(*) FROM appointments a
                        JOIN patients p ON p.id=a.id_patient
                       WHERE a.tenant_id<>p.tenant_id)
                    + (SELECT COUNT(*) FROM contemplations c
                        JOIN medical_slots m ON m.id=c.id_available_medical_slot
                       WHERE c.tenant_id<>m.tenant_id)
                    + (SELECT COUNT(*) FROM medical_slots
                       WHERE current_slots<0 OR current_slots>total_slots)
                    """)).isZero();

            assertThatThrownBy(() -> connection.createStatement().executeUpdate("""
                    INSERT INTO patients (
                      id, tenant_id, name, birth_date, gender, social_sit_rating,
                      sus_card_number, cpf, phone_number, address_street,
                      id_basic_health_unit, creation_date, creation_user
                    ) VALUES (
                      999999, 1, 'Cross tenant', '1980-01-01', 'Feminino', 1,
                      '174224524520048', '52998224725', '81999991234', 'Rua Teste',
                      101, NOW(6), 'security-test'
                    )
                    """))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    @Order(2)
    void updatesAtomicosImpedemSaldoNegativoEChaveDoJobDuplicada() throws Exception {
        String initialToken = "10000000-0000-0000-0000-000000000001";
        String firstReclaimToken = "10000000-0000-0000-0000-000000000011";
        String secondReclaimToken = "10000000-0000-0000-0000-000000000012";
        long slotId;
        try (Connection setup = connection()) {
            slotId = singleLong(setup, "SELECT MIN(id) FROM medical_slots");
            setup.createStatement().executeUpdate(
                    "UPDATE medical_slots SET current_slots=1 WHERE id=" + slotId);
        }

        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> decrement = () -> {
                start.await();
                try (Connection connection = connection();
                        var statement = connection.prepareStatement("""
                                UPDATE medical_slots
                                   SET current_slots=current_slots-1
                                 WHERE id=? AND current_slots>0
                                """)) {
                    statement.setLong(1, slotId);
                    return statement.executeUpdate();
                }
            };
            var first = executor.submit(decrement);
            var second = executor.submit(decrement);
            start.countDown();
            assertThat(first.get() + second.get()).isEqualTo(1);
        }

        try (Connection verify = connection()) {
            assertThat(singleLong(verify,
                    "SELECT current_slots FROM medical_slots WHERE id=" + slotId)).isZero();
            verify.createStatement().executeUpdate("""
                    INSERT INTO contemplation_job_executions
                      (tenant_id, execution_key, status, window_start, started_at,
                       lock_token, lease_until)
                    VALUES (1, 'security-it-key', 'RUNNING', NOW(6), NOW(6),
                            '%s',
                            DATE_ADD(NOW(6), INTERVAL 30 MINUTE))
                    """.formatted(initialToken));
            assertThatThrownBy(() -> verify.createStatement().executeUpdate("""
                    INSERT INTO contemplation_job_executions
                      (tenant_id, execution_key, status, window_start, started_at,
                       lock_token, lease_until)
                    VALUES (1, 'security-it-key', 'RUNNING', NOW(6), NOW(6),
                            '10000000-0000-0000-0000-000000000002',
                            DATE_ADD(NOW(6), INTERVAL 30 MINUTE))
                    """))
                    .isInstanceOf(SQLException.class);

            assertThat(verify.createStatement().executeUpdate("""
                    UPDATE contemplation_job_executions
                       SET status='COMPLETED', finished_at=NOW(6)
                     WHERE tenant_id=1
                       AND execution_key='security-it-key'
                       AND status='RUNNING'
                       AND lock_token='10000000-0000-0000-0000-000000000099'
                    """)).isZero();
            assertThat(verify.createStatement().executeUpdate("""
                    UPDATE contemplation_job_executions
                       SET lease_until=DATE_SUB(NOW(6), INTERVAL 1 MINUTE)
                     WHERE tenant_id=1
                       AND execution_key='security-it-key'
                       AND status='RUNNING'
                    """)).isEqualTo(1);
        }

        CountDownLatch reclaimStart = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> firstReclaim = reclaim(
                    reclaimStart, firstReclaimToken);
            Callable<Integer> secondReclaim = reclaim(
                    reclaimStart, secondReclaimToken);
            var first = executor.submit(firstReclaim);
            var second = executor.submit(secondReclaim);
            reclaimStart.countDown();
            assertThat(first.get() + second.get()).isEqualTo(1);
        }

        try (Connection verify = connection()) {
            assertThat(singleString(verify, """
                    SELECT lock_token
                      FROM contemplation_job_executions
                     WHERE tenant_id=1
                       AND execution_key='security-it-key'
                    """))
                    .isIn(firstReclaimToken, secondReclaimToken);
            assertThat(verify.createStatement().executeUpdate("""
                    UPDATE contemplation_job_executions
                       SET status='COMPLETED', finished_at=NOW(6)
                     WHERE tenant_id=1
                       AND execution_key='security-it-key'
                       AND status='RUNNING'
                       AND lock_token='%s'
                    """.formatted(initialToken))).isZero();
        }
    }

    @Test
    @Order(3)
    void configuracaoDuravelPreservaCommitAposRestart() throws Exception {
        try (Connection connection = connection()) {
            assertThat(singleLong(connection,
                    "SELECT @@GLOBAL.innodb_flush_log_at_trx_commit")).isEqualTo(1);
            assertThat(singleLong(connection, "SELECT @@GLOBAL.sync_binlog")).isEqualTo(1);
            connection.createStatement().executeUpdate("""
                    UPDATE system_maintenance
                       SET message='committed-before-restart'
                     WHERE id=1
                    """);
        }

        MYSQL.getDockerClient().restartContainerCmd(MYSQL.getContainerId())
                .withTimeout(30)
                .exec();

        Exception lastFailure = null;
        for (int attempt = 0; attempt < 60; attempt++) {
            try {
                var result = MYSQL.execInContainer(
                        "mysql",
                        "-h127.0.0.1",
                        "-usigaubs_test",
                        "-psecurity_test_only",
                        "--batch",
                        "--skip-column-names",
                        "sigaubs",
                        "-e",
                        "SELECT message FROM system_maintenance WHERE id=1");
                if (result.getExitCode() == 0) {
                    assertThat(result.getStdout().trim())
                            .isEqualTo("committed-before-restart");
                    return;
                }
                lastFailure = new IllegalStateException(result.getStderr());
            } catch (Exception exception) {
                lastFailure = exception;
            }
            Thread.sleep(1_000);
        }
        throw new AssertionError(
                "MySQL não retornou após o reinício controlado:\n" + MYSQL.getLogs(),
                lastFailure);
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    }

    private static long singleLong(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement();
                var result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private static String singleString(Connection connection, String sql)
            throws SQLException {
        try (var statement = connection.createStatement();
                var result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }

    private static Callable<Integer> reclaim(
            CountDownLatch start, String lockToken) {
        return () -> {
            start.await();
            try (Connection connection = connection();
                    var statement = connection.prepareStatement("""
                            UPDATE contemplation_job_executions
                               SET started_at=NOW(6),
                                   finished_at=NULL,
                                   lock_token=?,
                                   lease_until=DATE_ADD(NOW(6), INTERVAL 30 MINUTE)
                             WHERE tenant_id=1
                               AND execution_key='security-it-key'
                               AND status='RUNNING'
                               AND lease_until<NOW(6)
                            """)) {
                statement.setString(1, lockToken);
                return statement.executeUpdate();
            }
        };
    }

    private static String absolute(String relative) {
        return Path.of(relative).toAbsolutePath().normalize().toString();
    }
}
