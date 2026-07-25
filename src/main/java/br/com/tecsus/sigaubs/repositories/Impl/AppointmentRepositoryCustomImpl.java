package br.com.tecsus.sigaubs.repositories.Impl;

import br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO;
import br.com.tecsus.sigaubs.entities.Appointment;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.repositories.AppointmentRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaContext;

import java.time.LocalDateTime;
import java.util.List;

import static br.com.tecsus.sigaubs.utils.DefaultValues.QUATRO_MESES;


public class AppointmentRepositoryCustomImpl implements AppointmentRepositoryCustom {

    private final EntityManager entityManager;

    @Autowired
    public AppointmentRepositoryCustomImpl(JpaContext jpaContext) {
        this.entityManager = jpaContext.getEntityManagerByManagedType(Appointment.class);
    }

    @Override
    public Page<PatientOpenAppointmentDTO> findOpenAppointmentsQueuePaginated(ProcedureType type, Long ubsId, Long specialtyId, Pageable pageable) {

        TypedQuery<Long> openAppointmentsIdsQueryPaginated = entityManager.createQuery("""
            SELECT
                a.id
            FROM
                Appointment a
            LEFT JOIN a.medicalProcedure mp
            LEFT JOIN mp.specialty s
            LEFT JOIN a.patient p
            LEFT JOIN p.basicHealthUnit ubs
            WHERE a.contemplation IS NULL
                AND s.id = :specialtyId
                AND mp.procedureType = :type
                AND ubs.id = :ubsId
                AND a.status = br.com.tecsus.sigaubs.enums.AppointmentStatus.AGUARDANDO_CONTEMPLACAO
            ORDER BY
                    a.priority ASC,
                    p.birthDate ASC,
                    p.socialSituationRating ASC,
                    a.requestDate ASC
        """, Long.class);
        //   CASE WHEN a.priority = br.com.tecsus.sigaubs.enums.Priorities.ELETIVO THEN p.birthDate END ASC,
        //   CASE WHEN a.priority = br.com.tecsus.sigaubs.enums.Priorities.ELETIVO THEN p.socialSituationRating END ASC,


        openAppointmentsIdsQueryPaginated.setParameter("specialtyId", specialtyId);
        openAppointmentsIdsQueryPaginated.setParameter("type", type);
        openAppointmentsIdsQueryPaginated.setParameter("ubsId", ubsId);

        openAppointmentsIdsQueryPaginated.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());
        openAppointmentsIdsQueryPaginated.setMaxResults(pageable.getPageSize());

        var openAppointmentsQueueIdsPaginated = openAppointmentsIdsQueryPaginated.getResultList();
        long totalCountOpenAppointmentsQueue = openAppointmentsQueueIdsPaginated.size();

        if (openAppointmentsQueueIdsPaginated.size() >= pageable.getPageSize()) {
            TypedQuery<Long> count = entityManager.createQuery("""
                SELECT
                    COUNT(a.id)
                FROM
                    Appointment a
                LEFT JOIN a.medicalProcedure mp
                LEFT JOIN mp.specialty s
                LEFT JOIN a.patient p
                LEFT JOIN p.basicHealthUnit ubs
                WHERE a.contemplation IS NULL
                    AND s.id = :specialtyId
                    AND mp.procedureType = :type
                    AND ubs.id = :ubsId
                    AND a.status = br.com.tecsus.sigaubs.enums.AppointmentStatus.AGUARDANDO_CONTEMPLACAO
            """, Long.class);

            count.setParameter("specialtyId", specialtyId);
            count.setParameter("type", type);
            count.setParameter("ubsId", ubsId);

            totalCountOpenAppointmentsQueue = count.getSingleResult();
        }

        TypedQuery<PatientOpenAppointmentDTO> openAppointmentsQueueQuery = entityManager.createQuery("""
                SELECT
                    new br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO(
                    a.requestDate,
                    a.priority,
                    mp.procedureType,
                    mp.id,
                    mp.description,
                    s.title,
                    null,
                    COALESCE(a.observation, 'Sem observações.'),
                    a.id,
                    p.id,
                    p.name,
                    p.cpf,
                    p.gender,
                    p.birthDate,
                    p.socialSituationRating)
                FROM
                    Appointment a
                LEFT JOIN a.medicalProcedure mp
                LEFT JOIN mp.specialty s
                LEFT JOIN a.patient p
                WHERE a.id IN :ids
            """, PatientOpenAppointmentDTO.class);

        openAppointmentsQueueQuery.setParameter("ids", openAppointmentsQueueIdsPaginated);
        var openAppointmentsQueue = openAppointmentsQueueQuery.getResultList();

        return new PageImpl<>(openAppointmentsQueue, pageable, totalCountOpenAppointmentsQueue);
    }


    @Override
    public Page<PatientOpenAppointmentDTO> findOpenAppointmentsQueuePaginatedV2(Long ubsId, Long specialtyId, Long medicalProcedureId, Pageable pageable) {

        LocalDateTime dateLimit = LocalDateTime.now().minusMonths(QUATRO_MESES);
        String filters = buildQueueV2Filters(ubsId, specialtyId, medicalProcedureId);

        TypedQuery<Long> idsQuery = entityManager.createQuery("""
            SELECT a.id FROM Appointment a
            LEFT JOIN a.medicalProcedure mp
            LEFT JOIN mp.specialty s
            LEFT JOIN a.patient p
            LEFT JOIN p.basicHealthUnit ubs
            """ + filters + """
            ORDER BY
                ubs.name ASC,
                s.title ASC,
                mp.description ASC,
                CASE WHEN a.requestDate <= :dateLimit THEN 1 ELSE 2 END ASC,
                a.priority ASC,
                p.birthDate ASC,
                p.socialSituationRating ASC,
                a.requestDate ASC
        """, Long.class);

        setQueueV2FilterParameters(idsQuery, ubsId, specialtyId, medicalProcedureId);
        idsQuery.setParameter("dateLimit", dateLimit);
        idsQuery.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());
        idsQuery.setMaxResults(pageable.getPageSize());

        var ids = idsQuery.getResultList();
        long totalCount = ids.size();

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, totalCount);
        }

        if (ids.size() >= pageable.getPageSize()) {
            TypedQuery<Long> countQuery = entityManager.createQuery("""
                SELECT COUNT(a.id) FROM Appointment a
                LEFT JOIN a.medicalProcedure mp
                LEFT JOIN mp.specialty s
                LEFT JOIN a.patient p
                LEFT JOIN p.basicHealthUnit ubs
                """ + filters + """
            """, Long.class);

            setQueueV2FilterParameters(countQuery, ubsId, specialtyId, medicalProcedureId);
            totalCount = countQuery.getSingleResult();
        }

        TypedQuery<PatientOpenAppointmentDTO> queueQuery = entityManager.createQuery("""
            SELECT new br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO(
                a.requestDate, a.priority, mp.procedureType, mp.id, mp.description,
                s.title, ubs.name, COALESCE(a.observation, 'Sem observações.'),
                a.id, p.id, p.name, p.cpf, p.gender, p.birthDate, p.socialSituationRating)
            FROM Appointment a
            LEFT JOIN a.medicalProcedure mp
            LEFT JOIN mp.specialty s
            LEFT JOIN a.patient p
            LEFT JOIN p.basicHealthUnit ubs
            WHERE a.id IN :ids
            ORDER BY
                ubs.name ASC,
                s.title ASC,
                mp.description ASC,
                CASE WHEN a.requestDate <= :dateLimit THEN 1 ELSE 2 END ASC,
                a.priority ASC,
                p.birthDate ASC,
                p.socialSituationRating ASC,
                a.requestDate ASC
        """, PatientOpenAppointmentDTO.class);

        queueQuery.setParameter("ids", ids);
        queueQuery.setParameter("dateLimit", dateLimit);
        var openAppointmentsQueue = queueQuery.getResultList();

        return new PageImpl<>(openAppointmentsQueue, pageable, totalCount);
    }

    private String buildQueueV2Filters(Long ubsId, Long specialtyId, Long medicalProcedureId) {
        StringBuilder filters = new StringBuilder("""
            WHERE a.contemplation IS NULL
                AND a.status = br.com.tecsus.sigaubs.enums.AppointmentStatus.AGUARDANDO_CONTEMPLACAO
            """);

        if (ubsId != null) {
            filters.append("AND ubs.id = :ubsId\n");
        }
        if (specialtyId != null) {
            filters.append("AND s.id = :specialtyId\n");
        }
        if (medicalProcedureId != null) {
            filters.append("AND mp.id = :medicalProcedureId\n");
        }

        return filters.toString();
    }

    private void setQueueV2FilterParameters(TypedQuery<?> query, Long ubsId, Long specialtyId, Long medicalProcedureId) {
        if (ubsId != null) {
            query.setParameter("ubsId", ubsId);
        }
        if (specialtyId != null) {
            query.setParameter("specialtyId", specialtyId);
        }
        if (medicalProcedureId != null) {
            query.setParameter("medicalProcedureId", medicalProcedureId);
        }
    }
}
