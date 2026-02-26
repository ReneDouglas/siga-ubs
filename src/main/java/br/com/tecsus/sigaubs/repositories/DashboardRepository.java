package br.com.tecsus.sigaubs.repositories;

import br.com.tecsus.sigaubs.dtos.*;
import br.com.tecsus.sigaubs.enums.AppointmentStatus;
import br.com.tecsus.sigaubs.enums.Priorities;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
        public List<UBSSummaryDTO> findAllUBSSummaries() {

                String sql = """
                                SELECT
                                    bhu.id,
                                    bhu.name,
                                    bhu.neighborhood,
                                    (SELECT COUNT(a.id) FROM appointments a
                                     JOIN patients p ON a.id_patient = p.id
                                     WHERE p.id_basic_health_unit = bhu.id
                                     AND a.status = 'Aguardando Contemplação') AS total_open_appointments,
                                    (SELECT COUNT(c.id) FROM contemplations c
                                     JOIN medical_slots ms ON c.id_available_medical_slot = ms.id
                                     WHERE ms.id_basic_health_unit = bhu.id
                                     AND MONTH(ms.reference_month) = MONTH(NOW())
                                     AND YEAR(ms.reference_month) = YEAR(NOW())) AS total_contemplated,
                                    (SELECT COUNT(p.id) FROM patients p
                                     WHERE p.id_basic_health_unit = bhu.id) AS total_patients,
                                    (SELECT COALESCE(SUM(ms.current_slots), 0) FROM medical_slots ms
                                     WHERE ms.id_basic_health_unit = bhu.id
                                     AND MONTH(ms.reference_month) = MONTH(NOW())) AS total_available_slots,
                                    (SELECT COALESCE(ROUND(AVG(DATEDIFF(c.contemplation_date, a.request_date))), 0)
                                     FROM contemplations c
                                     JOIN appointments a ON a.id_contemplation = c.id
                                     JOIN patients p ON a.id_patient = p.id
                                     WHERE p.id_basic_health_unit = bhu.id
                                     AND c.contemplation_date >= DATE_SUB(NOW(), INTERVAL 6 MONTH)) AS average_wait_days
                                FROM basic_health_units bhu
                                ORDER BY bhu.name
                                """;

                List<Object[]> results = em.createNativeQuery(sql).getResultList();

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

                String sql = """
                                SELECT DATE_FORMAT(a.request_date, '%d/%m') AS dia, COUNT(a.id) AS total
                                FROM appointments a
                                WHERE a.request_date >= DATE_SUB(NOW(), INTERVAL 7 DAY)
                                GROUP BY DATE(a.request_date), DATE_FORMAT(a.request_date, '%d/%m')
                                ORDER BY DATE(a.request_date)
                                """;

                List<Object[]> results = em.createNativeQuery(sql).getResultList();

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

                String sql = """
                                SELECT DATE_FORMAT(a.request_date, '%b/%Y') AS mes, COUNT(a.id) AS total
                                FROM appointments a
                                WHERE a.status = 'Aguardando Contemplação'
                                AND a.request_date >= DATE_SUB(NOW(), INTERVAL 6 MONTH)
                                GROUP BY DATE_FORMAT(a.request_date, '%Y-%m'), DATE_FORMAT(a.request_date, '%b/%Y')
                                ORDER BY DATE_FORMAT(a.request_date, '%Y-%m')
                                """;

                List<Object[]> results = em.createNativeQuery(sql).getResultList();

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

                String sql = """
                                SELECT DATE_FORMAT(c.contemplation_date, '%b/%Y') AS mes, COUNT(c.id) AS total
                                FROM contemplations c
                                WHERE c.contemplation_date >= DATE_SUB(NOW(), INTERVAL 6 MONTH)
                                GROUP BY DATE_FORMAT(c.contemplation_date, '%Y-%m'), DATE_FORMAT(c.contemplation_date, '%b/%Y')
                                ORDER BY DATE_FORMAT(c.contemplation_date, '%Y-%m')
                                """;

                List<Object[]> results = em.createNativeQuery(sql).getResultList();

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

                String sql = """
                                SELECT a.priority AS prioridade, COUNT(a.id) AS total
                                FROM appointments a
                                WHERE a.status = 'Aguardando Contemplação'
                                AND a.priority IN (2, 3, 4, 8, 9)
                                GROUP BY a.priority
                                ORDER BY total DESC
                                """;

                List<Object[]> results = em.createNativeQuery(sql).getResultList();

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

                String sql = """
                                SELECT mp.type AS tipo, COUNT(a.id) AS total
                                FROM appointments a
                                JOIN medical_procedures mp ON a.id_medical_procedure = mp.id
                                WHERE a.status = 'Aguardando Contemplação'
                                GROUP BY mp.type
                                ORDER BY total DESC
                                """;

                List<Object[]> results = em.createNativeQuery(sql).getResultList();

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

                String sql = """
                                SELECT COUNT(c.id)
                                FROM contemplations c
                                WHERE DATE(c.contemplation_date) = CURDATE()
                                """;

                Object result = em.createNativeQuery(sql).getSingleResult();
                return ((Number) result).longValue();
        }

        /**
         * Retorna resumo de indicadores de uma UBS específica.
         */
        @SuppressWarnings("unchecked")
        public Object[] findUBSSummaryByUbsId(Long ubsId) {

                String sql = """
                                SELECT
                                    bhu.name,
                                    (SELECT COUNT(a.id) FROM appointments a
                                     JOIN patients p ON a.id_patient = p.id
                                     WHERE p.id_basic_health_unit = :ubsId
                                     AND a.status = 'Aguardando Contemplação') AS total_open,
                                    (SELECT COUNT(c.id) FROM contemplations c
                                     JOIN medical_slots ms ON c.id_available_medical_slot = ms.id
                                     WHERE ms.id_basic_health_unit = :ubsId
                                     AND MONTH(ms.reference_month) = MONTH(NOW())
                                     AND YEAR(ms.reference_month) = YEAR(NOW())) AS total_contemplated,
                                    (SELECT COUNT(p.id) FROM patients p
                                     WHERE p.id_basic_health_unit = :ubsId) AS total_patients
                                FROM basic_health_units bhu
                                WHERE bhu.id = :ubsId
                                """;

                List<Object[]> results = em.createNativeQuery(sql)
                                .setParameter("ubsId", ubsId)
                                .getResultList();

                return results.isEmpty() ? null : results.get(0);
        }

        /**
         * Retorna pacientes contemplados no mês corrente para uma UBS.
         */
        @SuppressWarnings("unchecked")
        public List<ContemplatedPatientRowDTO> findContemplatedPatientsByUbsThisMonth(Long ubsId) {

                String sql = """
                                SELECT
                                    p.name,
                                    s.title,
                                    mp.type,
                                    mp.description,
                                    a.status
                                FROM contemplations c
                                JOIN medical_slots ms   ON c.id_available_medical_slot = ms.id
                                JOIN medical_procedures mp ON ms.id_medical_procedure = mp.id
                                JOIN specialties s         ON mp.id_specialty = s.id
                                JOIN appointments a        ON a.id_contemplation = c.id
                                JOIN patients p            ON a.id_patient = p.id
                                WHERE ms.id_basic_health_unit = :ubsId
                                  AND MONTH(ms.reference_month) = MONTH(NOW())
                                  AND YEAR(ms.reference_month) = YEAR(NOW())
                                ORDER BY c.contemplation_date DESC
                                """;

                List<Object[]> results = em.createNativeQuery(sql)
                                .setParameter("ubsId", ubsId)
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
}
