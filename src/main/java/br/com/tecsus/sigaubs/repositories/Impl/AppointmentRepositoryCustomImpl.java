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

        TypedQuery<Long> idsQuery = entityManager.createQuery("""
            SELECT a.id FROM Appointment a
            LEFT JOIN a.medicalProcedure mp
            LEFT JOIN mp.specialty s
            LEFT JOIN a.patient p
            LEFT JOIN p.basicHealthUnit ubs
            WHERE a.contemplation IS NULL
                AND (:ubsId IS NULL OR ubs.id = :ubsId)
                AND (:specialtyId IS NULL OR s.id = :specialtyId)
                AND (:medicalProcedureId IS NULL OR mp.id = :medicalProcedureId)
                AND a.status = br.com.tecsus.sigaubs.enums.AppointmentStatus.AGUARDANDO_CONTEMPLACAO
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

        idsQuery.setParameter("ubsId", ubsId);
        idsQuery.setParameter("specialtyId", specialtyId);
        idsQuery.setParameter("medicalProcedureId", medicalProcedureId);
        idsQuery.setParameter("dateLimit", LocalDateTime.now().minusMonths(QUATRO_MESES));
        idsQuery.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());
        idsQuery.setMaxResults(pageable.getPageSize());

        var ids = idsQuery.getResultList();
        long totalCount = ids.size();

        if (ids.size() >= pageable.getPageSize()) {
            TypedQuery<Long> countQuery = entityManager.createQuery("""
                SELECT COUNT(a.id) FROM Appointment a
                LEFT JOIN a.medicalProcedure mp
                LEFT JOIN mp.specialty s
                LEFT JOIN a.patient p
                LEFT JOIN p.basicHealthUnit ubs
                WHERE a.contemplation IS NULL
                    AND (:ubsId IS NULL OR ubs.id = :ubsId)
                    AND (:specialtyId IS NULL OR s.id = :specialtyId)
                    AND (:medicalProcedureId IS NULL OR mp.id = :medicalProcedureId)
                    AND a.status = br.com.tecsus.sigaubs.enums.AppointmentStatus.AGUARDANDO_CONTEMPLACAO
            """, Long.class);

            countQuery.setParameter("ubsId", ubsId);
            countQuery.setParameter("specialtyId", specialtyId);
            countQuery.setParameter("medicalProcedureId", medicalProcedureId);
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
        queueQuery.setParameter("dateLimit", LocalDateTime.now().minusMonths(QUATRO_MESES));
        var openAppointmentsQueue = queueQuery.getResultList();

        return new PageImpl<>(openAppointmentsQueue, pageable, totalCount);
    }
}
