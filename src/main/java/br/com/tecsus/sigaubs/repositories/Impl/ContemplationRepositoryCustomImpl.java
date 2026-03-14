package br.com.tecsus.sigaubs.repositories.Impl;

import br.com.tecsus.sigaubs.entities.Contemplation;
import br.com.tecsus.sigaubs.enums.AppointmentStatus;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.repositories.ContemplationRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

public class ContemplationRepositoryCustomImpl implements ContemplationRepositoryCustom {

    private final EntityManager em;

    @Autowired
    public ContemplationRepositoryCustomImpl(JpaContext jpa) {
        this.em = jpa.getEntityManagerByManagedType(Contemplation.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Contemplation> findConsultationsByUBSAndSpecialtyPaginated(ProcedureType type,
                                                                           Long ubsId,
                                                                           Long specialtyId,
                                                                           YearMonth referenceMonth,
                                                                           AppointmentStatus status,
                                                                           Pageable pageable) {

        StringBuilder queryBuilder = new StringBuilder();

        queryBuilder.append("SELECT c.id ");
        queryBuilder.append("FROM Contemplation c ");
        queryBuilder.append("JOIN c.medicalSlot ms ");
        queryBuilder.append("JOIN ms.basicHealthUnit ubs ");
        queryBuilder.append("JOIN ms.medicalProcedure mp ");
        queryBuilder.append("JOIN c.appointment a ");
        queryBuilder.append("JOIN a.patient p ");
        queryBuilder.append("JOIN mp.specialty s ");
        if (referenceMonth == null) queryBuilder.append("WHERE 1=1 ");
        if (referenceMonth != null) queryBuilder.append("WHERE MONTH(ms.referenceMonth) = :month ");
        if (referenceMonth != null) queryBuilder.append("AND YEAR(ms.referenceMonth) = :year ");
        if (ubsId != null) queryBuilder.append("AND ubs.id = :ubsId ");
        if (specialtyId != null) queryBuilder.append("AND s.id = :specialtyId ");
        queryBuilder.append("AND mp.procedureType = :type ");

        if (status == null) {
            queryBuilder.append("AND a.status <> br.com.tecsus.sigaubs.enums.AppointmentStatus.AGUARDANDO_CONTEMPLACAO ");
            queryBuilder.append("AND a.status <> br.com.tecsus.sigaubs.enums.AppointmentStatus.CONTEMPLACAO_CANCELADA ");
            queryBuilder.append("AND a.status <> br.com.tecsus.sigaubs.enums.AppointmentStatus.ATENDIMENTO_CONCLUIDO ");
        }
        else {
            queryBuilder.append("AND a.status = :status ");
        }

        queryBuilder.append("ORDER BY ");
        queryBuilder.append("c.contemplationDate DESC, ");
        if (ubsId == null) queryBuilder.append("ubs.name, ");
        if (specialtyId == null) queryBuilder.append("s.title, ");
        queryBuilder.append("mp.description, ");
        queryBuilder.append("p.name ");

        TypedQuery<Long> contemplationIdsQueryPaginated = em.createQuery(queryBuilder.toString(), Long.class);

        /*TypedQuery<Long> contemplationIdsQueryPaginated = em.createQuery("""
            SELECT c.id
            FROM Contemplation c
            JOIN c.medicalSlot ms
            JOIN ms.basicHealthUnit ubs
            JOIN ms.medicalProcedure mp
            JOIN c.appointment a
            JOIN a.patient p
            JOIN mp.specialty s
            WHERE ubs.id = :ubsId
            AND s.id = :specialtyId
            AND mp.procedureType = :type
            AND (:status IS NULL OR c.status = :status)
            AND MONTH(ms.referenceMonth) = :month
            AND YEAR(ms.referenceMonth) = :year
            ORDER BY mp.description, p.name
        """, Long.class);*/

        if (ubsId != null) contemplationIdsQueryPaginated.setParameter("ubsId", ubsId);
        if (specialtyId != null) contemplationIdsQueryPaginated.setParameter("specialtyId", specialtyId);
        contemplationIdsQueryPaginated.setParameter("type", type);
        if (status != null) contemplationIdsQueryPaginated.setParameter("status", status);
        if (referenceMonth != null) contemplationIdsQueryPaginated.setParameter("month", referenceMonth.getMonthValue());
        if (referenceMonth != null) contemplationIdsQueryPaginated.setParameter("year", referenceMonth.getYear());

        contemplationIdsQueryPaginated.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());
        contemplationIdsQueryPaginated.setMaxResults(pageable.getPageSize());

        var contemplationIdsPaginated = contemplationIdsQueryPaginated.getResultList();
        long totalCountContemplations = contemplationIdsPaginated.size();

        if (contemplationIdsPaginated.size() >= pageable.getPageSize()) {

            StringBuilder countBuilder = new StringBuilder();

            countBuilder.append("SELECT COUNT(c.id) ");
            countBuilder.append("FROM Contemplation c ");
            countBuilder.append("JOIN c.medicalSlot ms ");
            countBuilder.append("JOIN ms.basicHealthUnit ubs ");
            countBuilder.append("JOIN ms.medicalProcedure mp ");
            countBuilder.append("JOIN c.appointment a ");
            countBuilder.append("JOIN a.patient p ");
            countBuilder.append("JOIN mp.specialty s ");
            if (referenceMonth == null) countBuilder.append("WHERE 1=1 ");
            if (referenceMonth != null) countBuilder.append("WHERE MONTH(ms.referenceMonth) = :month ");
            if (referenceMonth != null) countBuilder.append("AND YEAR(ms.referenceMonth) = :year ");
            if (ubsId != null) countBuilder.append("AND ubs.id = :ubsId ");
            if (specialtyId != null) countBuilder.append("AND s.id = :specialtyId ");
            countBuilder.append("AND mp.procedureType = :type ");
            if (status == null) {
                countBuilder.append("AND a.status <> br.com.tecsus.sigaubs.enums.AppointmentStatus.AGUARDANDO_CONTEMPLACAO ");
                countBuilder.append("AND a.status <> br.com.tecsus.sigaubs.enums.AppointmentStatus.CONTEMPLACAO_CANCELADA ");
                countBuilder.append("AND a.status <> br.com.tecsus.sigaubs.enums.AppointmentStatus.ATENDIMENTO_CONCLUIDO ");
            }
            else {
                countBuilder.append("AND a.status = :status ");
            }

            TypedQuery<Long> count = em.createQuery(countBuilder.toString(), Long.class);

            /*TypedQuery<Long> count = em.createQuery("""
                SELECT COUNT(c.id)
                FROM Contemplation c
                JOIN c.medicalSlot ms
                JOIN ms.basicHealthUnit ubs
                JOIN ms.medicalProcedure mp
                JOIN c.appointment a
                JOIN a.patient p
                JOIN mp.specialty s
                WHERE ubs.id = :ubsId
                AND s.id = :specialtyId
                AND mp.procedureType = :type
                AND (:status IS NULL OR c.status = :status)
                AND MONTH(ms.referenceMonth) = :month
                AND YEAR(ms.referenceMonth) = :year
            """, Long.class);*/

            if (ubsId != null) count.setParameter("ubsId", ubsId);
            if (specialtyId != null) count.setParameter("specialtyId", specialtyId);
            count.setParameter("type", type);
            if (status != null) count.setParameter("status", status);
            if (referenceMonth != null) count.setParameter("month", referenceMonth.getMonthValue());
            if (referenceMonth != null) count.setParameter("year", referenceMonth.getYear());

            totalCountContemplations = count.getSingleResult();
        }

        StringBuilder cBuilder = new StringBuilder();

        cBuilder.append("SELECT c ");
        cBuilder.append("FROM Contemplation c ");
        cBuilder.append("JOIN FETCH c.medicalSlot ms ");
        cBuilder.append("JOIN FETCH ms.basicHealthUnit ubs ");
        cBuilder.append("JOIN FETCH ms.medicalProcedure mp ");
        cBuilder.append("JOIN FETCH c.appointment a ");
        cBuilder.append("JOIN FETCH a.patient p ");
        cBuilder.append("JOIN FETCH mp.specialty s ");
        cBuilder.append("WHERE c.id IN :contemplationIds ");
        cBuilder.append("ORDER BY ");
        cBuilder.append("c.contemplationDate DESC, ");
        if (ubsId == null) cBuilder.append("ubs.name, ");
        if (specialtyId == null) cBuilder.append("s.title, ");
        cBuilder.append("mp.description, ");
        cBuilder.append("p.name ");

        TypedQuery<Contemplation> contemplationsQuery = em.createQuery(cBuilder.toString(), Contemplation.class);

        /*TypedQuery<Contemplation> contemplationsQuery = em.createQuery("""
            SELECT c
            FROM Contemplation c
            JOIN FETCH c.medicalSlot ms
            JOIN FETCH ms.basicHealthUnit ubs
            JOIN FETCH ms.medicalProcedure mp
            JOIN FETCH c.appointment a
            JOIN FETCH a.patient p
            JOIN FETCH mp.specialty s
            WHERE c.id IN :contemplationIds
        """, Contemplation.class);*/

        contemplationsQuery.setParameter("contemplationIds", contemplationIdsPaginated);
        var contemplations = contemplationsQuery.getResultList();

        return new PageImpl<>(contemplations, pageable, totalCountContemplations);

    }
}
