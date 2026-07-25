package br.com.tecsus.sigaubs.repositories;

import br.com.tecsus.sigaubs.dtos.*;
import br.com.tecsus.sigaubs.enums.AppointmentStatus;
import br.com.tecsus.sigaubs.enums.Priorities;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.tenancy.TenantContextHolder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class DashboardRepository {

        @PersistenceContext
        private EntityManager em;

        /**
         * Q1 + Q2 consolidada: Retorna resumo de todas as UBS com indicadores.
         * Usa native query MySQL com subconsultas correlatas para evitar N+1.
         */
        @SuppressWarnings("unchecked")
        public List<UBSSummaryDTO> findAllUBSSummaries(LocalDate startOfMonth, LocalDate startOfNextMonth) {
                Long tenantId = TenantContextHolder.getRequiredTenantId();

                String sql = """
                                SELECT
                                    bhu.id,
                                    bhu.name,
                                    bhu.neighborhood,
                                    COALESCE(open_appts.total_open_appointments, 0) AS total_open_appointments,
                                    COALESCE(contemplated.total_contemplated, 0) AS total_contemplated,
                                    COALESCE(patients_total.total_patients, 0) AS total_patients,
                                    COALESCE(available_slots.total_available_slots, 0) AS total_available_slots,
                                    COALESCE(wait_times.average_wait_days, 0) AS average_wait_days
                                FROM basic_health_units bhu
                                LEFT JOIN (
                                    SELECT p.id_basic_health_unit AS ubs_id, COUNT(a.id) AS total_open_appointments
                                    FROM appointments a
                                    JOIN patients p ON a.id_patient = p.id AND p.tenant_id = a.tenant_id
                                    WHERE a.tenant_id = :tenantId
                                      AND a.status = 'Aguardando Contemplação'
                                    GROUP BY p.id_basic_health_unit
                                ) open_appts ON open_appts.ubs_id = bhu.id
                                LEFT JOIN (
                                    SELECT ms.id_basic_health_unit AS ubs_id, COUNT(c.id) AS total_contemplated
                                    FROM contemplations c
                                    JOIN medical_slots ms ON c.id_available_medical_slot = ms.id AND ms.tenant_id = c.tenant_id
                                    WHERE c.tenant_id = :tenantId
                                      AND ms.reference_month >= :startOfMonth
                                      AND ms.reference_month < :startOfNextMonth
                                    GROUP BY ms.id_basic_health_unit
                                ) contemplated ON contemplated.ubs_id = bhu.id
                                LEFT JOIN (
                                    SELECT p.id_basic_health_unit AS ubs_id, COUNT(p.id) AS total_patients
                                    FROM patients p
                                    WHERE p.tenant_id = :tenantId
                                    GROUP BY p.id_basic_health_unit
                                ) patients_total ON patients_total.ubs_id = bhu.id
                                LEFT JOIN (
                                    SELECT ms.id_basic_health_unit AS ubs_id, SUM(ms.current_slots) AS total_available_slots
                                    FROM medical_slots ms
                                    WHERE ms.tenant_id = :tenantId
                                      AND ms.reference_month >= :startOfMonth
                                      AND ms.reference_month < :startOfNextMonth
                                    GROUP BY ms.id_basic_health_unit
                                ) available_slots ON available_slots.ubs_id = bhu.id
                                LEFT JOIN (
                                    SELECT p.id_basic_health_unit AS ubs_id,
                                           ROUND(AVG(DATEDIFF(c.contemplation_date, a.request_date))) AS average_wait_days
                                    FROM contemplations c
                                    JOIN appointments a ON a.id_contemplation = c.id AND a.tenant_id = c.tenant_id
                                    JOIN patients p ON a.id_patient = p.id AND p.tenant_id = a.tenant_id
                                    WHERE c.tenant_id = :tenantId
                                      AND c.contemplation_date >= DATE_SUB(NOW(), INTERVAL 6 MONTH)
                                    GROUP BY p.id_basic_health_unit
                                ) wait_times ON wait_times.ubs_id = bhu.id
                                WHERE bhu.tenant_id = :tenantId
                                ORDER BY bhu.name
                                """;

                List<Object[]> results = em.createNativeQuery(sql)
                                .setParameter("tenantId", tenantId)
                                .setParameter("startOfMonth", startOfMonth)
                                .setParameter("startOfNextMonth", startOfNextMonth)
                                .getResultList();

                return results.stream()
                                .map(row -> new UBSSummaryDTO(
                                                ((Number) row[0]).longValue(),
                                                (String) row[1],
                                                (String) row[2],
                                                ((Number) row[3]).longValue(),
                                                ((Number) row[4]).longValue(),
                                                ((Number) row[5]).longValue(),
                                                ((Number) row[6]).longValue(),
                                                ((Number) row[7]).longValue()))
                                .toList();
        }

        /**
         * Q3: Marcações diárias nos últimos 7 dias.
         */
        @SuppressWarnings("unchecked")
        public List<DailyAppointmentDTO> findDailyAppointments() {
                Long tenantId = TenantContextHolder.getRequiredTenantId();

                String sql = """
                                SELECT DATE_FORMAT(a.request_date, '%d/%m') AS dia, COUNT(a.id) AS total
                                FROM appointments a
                                WHERE a.tenant_id = :tenantId
                                AND a.request_date >= DATE_SUB(NOW(), INTERVAL 7 DAY)
                                GROUP BY DATE(a.request_date), DATE_FORMAT(a.request_date, '%d/%m')
                                ORDER BY DATE(a.request_date)
                                """;

                List<Object[]> results = em.createNativeQuery(sql)
                                .setParameter("tenantId", tenantId)
                                .getResultList();

                return results.stream()
                                .map(row -> new DailyAppointmentDTO(
                                                (String) row[0],
                                                ((Number) row[1]).longValue()))
                                .toList();
        }

        /**
         * Q4a: Marcações em aberto por mês nos últimos 6 meses.
         */
        @SuppressWarnings("unchecked")
        public List<MonthlyStatsDTO> findMonthlyOpenAppointments() {
                Long tenantId = TenantContextHolder.getRequiredTenantId();

                String sql = """
                                SELECT DATE_FORMAT(a.request_date, '%b/%Y') AS mes, COUNT(a.id) AS total
                                FROM appointments a
                                WHERE a.tenant_id = :tenantId
                                AND a.status = 'Aguardando Contemplação'
                                AND a.request_date >= DATE_SUB(NOW(), INTERVAL 6 MONTH)
                                GROUP BY DATE_FORMAT(a.request_date, '%Y-%m'), DATE_FORMAT(a.request_date, '%b/%Y')
                                ORDER BY DATE_FORMAT(a.request_date, '%Y-%m')
                                """;

                List<Object[]> results = em.createNativeQuery(sql)
                                .setParameter("tenantId", tenantId)
                                .getResultList();

                return results.stream()
                                .map(row -> new MonthlyStatsDTO(
                                                (String) row[0],
                                                ((Number) row[1]).longValue()))
                                .toList();
        }

        /**
         * Q4b: Contemplados por mês nos últimos 6 meses.
         */
        @SuppressWarnings("unchecked")
        public List<MonthlyStatsDTO> findMonthlyContemplations() {
                Long tenantId = TenantContextHolder.getRequiredTenantId();

                String sql = """
                                SELECT DATE_FORMAT(c.contemplation_date, '%b/%Y') AS mes, COUNT(c.id) AS total
                                FROM contemplations c
                                WHERE c.tenant_id = :tenantId
                                AND c.contemplation_date >= DATE_SUB(NOW(), INTERVAL 6 MONTH)
                                GROUP BY DATE_FORMAT(c.contemplation_date, '%Y-%m'), DATE_FORMAT(c.contemplation_date, '%b/%Y')
                                ORDER BY DATE_FORMAT(c.contemplation_date, '%Y-%m')
                                """;

                List<Object[]> results = em.createNativeQuery(sql)
                                .setParameter("tenantId", tenantId)
                                .getResultList();

                return results.stream()
                                .map(row -> new MonthlyStatsDTO(
                                                (String) row[0],
                                                ((Number) row[1]).longValue()))
                                .toList();
        }

        /**
         * Q5: Distribuição de agendamentos por prioridade (fila ativa).
         */
        @SuppressWarnings("unchecked")
        public List<PriorityDistributionDTO> findPriorityDistribution() {
                Long tenantId = TenantContextHolder.getRequiredTenantId();

                String sql = """
                                SELECT a.priority AS prioridade, COUNT(a.id) AS total
                                FROM appointments a
                                WHERE a.tenant_id = :tenantId
                                AND a.status = 'Aguardando Contemplação'
                                AND a.priority IN (2, 3, 4, 8, 9)
                                GROUP BY a.priority
                                ORDER BY total DESC
                                """;

                List<Object[]> results = em.createNativeQuery(sql)
                                .setParameter("tenantId", tenantId)
                                .getResultList();

                return results.stream()
                                .map(row -> {
                                        int priorityValue = ((Number) row[0]).intValue();
                                        String label = java.util.Arrays.stream(Priorities.values())
                                                        .filter(p -> p.getValue() == priorityValue)
                                                        .findFirst()
                                                        .map(Priorities::getDescription)
                                                        .orElse("Desconhecido");
                                        return new PriorityDistributionDTO(label, ((Number) row[1]).longValue());
                                })
                                .toList();
        }

        /**
         * Q5b: Distribuição de agendamentos por tipo de procedimento (fila ativa).
         */
        @SuppressWarnings("unchecked")
        public List<ProcedureTypeDistributionDTO> findProcedureTypeDistribution() {
                Long tenantId = TenantContextHolder.getRequiredTenantId();

                String sql = """
                                SELECT mp.type AS tipo, COUNT(a.id) AS total
                                FROM appointments a
                                JOIN medical_procedures mp ON a.id_medical_procedure = mp.id
                                WHERE a.tenant_id = :tenantId
                                AND a.status = 'Aguardando Contemplação'
                                GROUP BY mp.type
                                ORDER BY total DESC
                                """;

                List<Object[]> results = em.createNativeQuery(sql)
                                .setParameter("tenantId", tenantId)
                                .getResultList();

                return results.stream()
                                .map(row -> {
                                        String typeValue = (String) row[0];
                                        String label = ProcedureType.valueOf(typeValue).getDescription();
                                        return new ProcedureTypeDistributionDTO(label, ((Number) row[1]).longValue());
                                })
                                .toList();
        }

        /**
         * Q6: Total de contemplados hoje.
         */
        public Long countTodayContemplations() {
                Long tenantId = TenantContextHolder.getRequiredTenantId();

                String sql = """
                                SELECT COUNT(c.id)
                                FROM contemplations c
                                WHERE c.tenant_id = :tenantId
                                AND c.contemplation_date >= CURDATE()
                                AND c.contemplation_date < DATE_ADD(CURDATE(), INTERVAL 1 DAY)
                                """;

                Object result = em.createNativeQuery(sql)
                                .setParameter("tenantId", tenantId)
                                .getSingleResult();
                return ((Number) result).longValue();
        }

        /**
         * Retorna resumo de indicadores de uma UBS específica.
         */
        public UBSSingleSummaryDTO findUBSSummaryByUbsId(Long ubsId, LocalDate startOfMonth, LocalDate startOfNextMonth) {
                Long tenantId = TenantContextHolder.getRequiredTenantId();

                String sql = """
                                SELECT
                                    bhu.name,
                                    (SELECT COUNT(a.id) FROM appointments a
                                     JOIN patients p ON a.id_patient = p.id AND p.tenant_id = a.tenant_id
                                     WHERE p.id_basic_health_unit = :ubsId
                                     AND a.tenant_id = :tenantId
                                     AND a.status = 'Aguardando Contemplação') AS total_open,
                                    (SELECT COUNT(c.id) FROM contemplations c
                                     JOIN medical_slots ms ON c.id_available_medical_slot = ms.id AND ms.tenant_id = c.tenant_id
                                     WHERE ms.id_basic_health_unit = :ubsId
                                     AND c.tenant_id = :tenantId
                                     AND ms.reference_month >= :startOfMonth
                                     AND ms.reference_month < :startOfNextMonth) AS total_contemplated,
                                    (SELECT COUNT(p.id) FROM patients p
                                     WHERE p.id_basic_health_unit = :ubsId
                                     AND p.tenant_id = :tenantId) AS total_patients
                                FROM basic_health_units bhu
                                WHERE bhu.id = :ubsId
                                AND bhu.tenant_id = :tenantId
                                """;

                List<Tuple> results = em.createNativeQuery(sql, Tuple.class)
                                .setParameter("ubsId", ubsId)
                                .setParameter("tenantId", tenantId)
                                .setParameter("startOfMonth", startOfMonth)
                                .setParameter("startOfNextMonth", startOfNextMonth)
                                .getResultList();

                if (results.isEmpty()) return null;
                Tuple row = results.get(0);
                return new UBSSingleSummaryDTO(
                                row.get("name", String.class),
                                ((Number) row.get("total_open")).longValue(),
                                ((Number) row.get("total_contemplated")).longValue(),
                                ((Number) row.get("total_patients")).longValue());
        }

        /**
         * Retorna pacientes contemplados no mês corrente para uma UBS.
         */
        @SuppressWarnings("unchecked")
        public List<ContemplatedPatientRowDTO> findContemplatedPatientsByUbsThisMonth(Long ubsId,
                        LocalDate startOfMonth, LocalDate startOfNextMonth) {
                Long tenantId = TenantContextHolder.getRequiredTenantId();

                String sql = """
                                SELECT
                                    p.name,
                                    s.title,
                                    mp.type,
                                    mp.description,
                                    a.status
                                FROM contemplations c
                                JOIN medical_slots ms   ON c.id_available_medical_slot = ms.id AND ms.tenant_id = c.tenant_id
                                JOIN medical_procedures mp ON ms.id_medical_procedure = mp.id
                                JOIN specialties s         ON mp.id_specialty = s.id
                                JOIN appointments a        ON a.id_contemplation = c.id AND a.tenant_id = c.tenant_id
                                JOIN patients p            ON a.id_patient = p.id AND p.tenant_id = a.tenant_id
                                WHERE ms.id_basic_health_unit = :ubsId
                                  AND c.tenant_id = :tenantId
                                  AND ms.reference_month >= :startOfMonth
                                  AND ms.reference_month < :startOfNextMonth
                                ORDER BY c.contemplation_date DESC
                                """;

                List<Object[]> results = em.createNativeQuery(sql)
                                .setParameter("ubsId", ubsId)
                                .setParameter("tenantId", tenantId)
                                .setParameter("startOfMonth", startOfMonth)
                                .setParameter("startOfNextMonth", startOfNextMonth)
                                .getResultList();

                return results.stream()
                                .map(row -> new ContemplatedPatientRowDTO(
                                                (String) row[0],
                                                (String) row[1],
                                                ProcedureType.valueOf((String) row[2]),
                                                (String) row[3],
                                                AppointmentStatus.getByDescription((String) row[4])))
                                .toList();
        }

        /**
         * Q7: Top 10 gargalos — especialidades/procedimentos com maior fila ativa.
         * Query única com JOINs, sem risco de N+1.
         */
        @SuppressWarnings("unchecked")
        public List<BottleneckDTO> findTopBottlenecks() {
                Long tenantId = TenantContextHolder.getRequiredTenantId();

                String sql = """
                                SELECT s.title, mp.description, COUNT(a.id) AS total_fila
                                FROM appointments a
                                JOIN medical_procedures mp ON a.id_medical_procedure = mp.id
                                JOIN specialties s ON mp.id_specialty = s.id
                                WHERE a.tenant_id = :tenantId
                                AND a.status = 'Aguardando Contemplação'
                                GROUP BY s.title, mp.description
                                ORDER BY total_fila DESC
                                LIMIT 10
                                """;

                List<Object[]> results = em.createNativeQuery(sql)
                                .setParameter("tenantId", tenantId)
                                .getResultList();

                return results.stream()
                                .map(row -> new BottleneckDTO(
                                                (String) row[0],
                                                (String) row[1],
                                                ((Number) row[2]).longValue()))
                                .toList();
        }

        /**
         * Q8: Taxa de ocupação de vagas por UBS no mês corrente.
         * Query única com JOIN + GROUP BY agregado, sem risco de N+1.
         */
        @SuppressWarnings("unchecked")
        public List<SlotOccupancyDTO> findSlotOccupancyByUBS(LocalDate startOfMonth, LocalDate startOfNextMonth) {
                Long tenantId = TenantContextHolder.getRequiredTenantId();

                String sql = """
                                SELECT bhu.name,
                                       COALESCE(SUM(ms.total_slots), 0) AS vagas_totais,
                                       COALESCE(SUM(ms.total_slots - ms.current_slots), 0) AS vagas_consumidas
                                FROM medical_slots ms
                                JOIN basic_health_units bhu ON ms.id_basic_health_unit = bhu.id AND bhu.tenant_id = ms.tenant_id
                                WHERE ms.tenant_id = :tenantId
                                  AND ms.reference_month >= :startOfMonth
                                  AND ms.reference_month < :startOfNextMonth
                                GROUP BY bhu.name
                                ORDER BY bhu.name
                                """;

                List<Object[]> results = em.createNativeQuery(sql)
                                .setParameter("tenantId", tenantId)
                                .setParameter("startOfMonth", startOfMonth)
                                .setParameter("startOfNextMonth", startOfNextMonth)
                                .getResultList();

                return results.stream()
                                .map(row -> new SlotOccupancyDTO(
                                                (String) row[0],
                                                ((Number) row[1]).longValue(),
                                                ((Number) row[2]).longValue()))
                                .toList();
        }
}
