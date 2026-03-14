package br.com.tecsus.sigaubs.repositories.Impl;

import br.com.tecsus.sigaubs.dtos.PatientAppointmentsHistoryDTO;
import br.com.tecsus.sigaubs.entities.Patient;
import br.com.tecsus.sigaubs.repositories.PatientRepositoryCustom;
import br.com.tecsus.sigaubs.utils.ValidationUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaContext;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public class PatientRepositoryCustomImpl implements PatientRepositoryCustom {

    private final EntityManager em;
    private ValidationUtils validationUtils;

    @Autowired
    public PatientRepositoryCustomImpl(JpaContext jpaContext) {
        this.em = jpaContext.getEntityManagerByManagedType(Patient.class);
    }

    @Autowired
    public void setValidationUtils(ValidationUtils validationUtils) {
        this.validationUtils = validationUtils;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> searchNativePatientsContainingByUBS(String terms, Long idUBS) {

        String jpql = """
                SELECT p.*
                FROM patients p
                WHERE MATCH(p.name, p.sus_card_number, p.cpf) AGAINST(:terms IN BOOLEAN MODE)
                AND (:id IS NULL OR p.id_basic_health_unit = :id)
                LIMIT 5
                """;

        Query nativeQuery = em.createNativeQuery(jpql, Patient.class);
        nativeQuery.setParameter("terms", "*" + terms + "*");
        nativeQuery.setParameter("id", idUBS);

        var list = (List<Patient>) nativeQuery.getResultList();
        return list;
    }

    @Override
    public Page<Patient> findPatientsPaginated(Patient patient, Pageable page) {

        Long ubsId = validationUtils.attrIsNotNull(patient.getBasicHealthUnit().getId()) ? patient.getBasicHealthUnit().getId() : null;
        String name = validationUtils.attrIsNotNull(patient.getName()) ? "%" + patient.getName() + "%" : null;
        String phoneNumber = validationUtils.attrIsNotNull(patient.getPhoneNumber()) ? patient.getPhoneNumber() : null;
        String cpf = validationUtils.attrIsNotNull(patient.getCpf()) ? patient.getCpf() : null;
        String susNumber = validationUtils.attrIsNotNull(patient.getSusNumber()) ? patient.getSusNumber() : null;
        String addressStreet = validationUtils.attrIsNotNull(patient.getAddressStreet()) ? "%" + patient.getAddressStreet() + "%" : null;
        var socialSituationRating = validationUtils.attrIsNotNull(patient.getSocialSituationRating()) ? patient.getSocialSituationRating() : null;
        String acsName = validationUtils.attrIsNotNull(patient.getAcsName()) ? patient.getAcsName() : null;

        TypedQuery<Long> idsQuery = em.createQuery("""
            SELECT p.id FROM Patient p
            WHERE (:ubsId IS NULL OR p.basicHealthUnit.id = :ubsId)
                AND (:name IS NULL OR p.name LIKE :name)
                AND (:phoneNumber IS NULL OR p.phoneNumber = :phoneNumber)
                AND (:cpf IS NULL OR p.cpf = :cpf)
                AND (:susNumber IS NULL OR p.susNumber = :susNumber)
                AND (:addressStreet IS NULL OR p.addressStreet LIKE :addressStreet)
                AND (:socialSituationRating IS NULL OR p.socialSituationRating = :socialSituationRating)
                AND (:acsName IS NULL OR p.acsName = :acsName)
            ORDER BY p.name
        """, Long.class);

        idsQuery.setParameter("ubsId", ubsId);
        idsQuery.setParameter("name", name);
        idsQuery.setParameter("phoneNumber", phoneNumber);
        idsQuery.setParameter("cpf", cpf);
        idsQuery.setParameter("susNumber", susNumber);
        idsQuery.setParameter("addressStreet", addressStreet);
        idsQuery.setParameter("socialSituationRating", socialSituationRating);
        idsQuery.setParameter("acsName", acsName);
        idsQuery.setFirstResult(page.getPageNumber() * page.getPageSize());
        idsQuery.setMaxResults(page.getPageSize());

        var ids = idsQuery.getResultList();
        long totalCount = 0;

        if (ids.size() < page.getPageSize()) {
            totalCount = ids.size();
        } else {
            TypedQuery<Long> countQuery = em.createQuery("""
                SELECT COUNT(p.id) FROM Patient p
                WHERE (:ubsId IS NULL OR p.basicHealthUnit.id = :ubsId)
                    AND (:name IS NULL OR p.name LIKE :name)
                    AND (:phoneNumber IS NULL OR p.phoneNumber = :phoneNumber)
                    AND (:cpf IS NULL OR p.cpf = :cpf)
                    AND (:susNumber IS NULL OR p.susNumber = :susNumber)
                    AND (:addressStreet IS NULL OR p.addressStreet LIKE :addressStreet)
                    AND (:socialSituationRating IS NULL OR p.socialSituationRating = :socialSituationRating)
                    AND (:acsName IS NULL OR p.acsName = :acsName)
            """, Long.class);

            countQuery.setParameter("ubsId", ubsId);
            countQuery.setParameter("name", name);
            countQuery.setParameter("phoneNumber", phoneNumber);
            countQuery.setParameter("cpf", cpf);
            countQuery.setParameter("susNumber", susNumber);
            countQuery.setParameter("addressStreet", addressStreet);
            countQuery.setParameter("socialSituationRating", socialSituationRating);
            countQuery.setParameter("acsName", acsName);
            totalCount = countQuery.getSingleResult();
        }

        TypedQuery<Patient> patientsQuery = em.createQuery("""
            SELECT p FROM Patient p WHERE p.id IN :ids ORDER BY p.name
        """, Patient.class);

        patientsQuery.setParameter("ids", ids);
        List<Patient> patients = patientsQuery.getResultList();

        return new PageImpl<>(patients, page, totalCount);
    }

    @Override
    public Page<PatientAppointmentsHistoryDTO> findPatientAppointmentsHistoryPaginated(Patient patient, Pageable page) {

        TypedQuery<Long> appointmentsHistoryIdQueryPaginated = em.createQuery("""
                SELECT
                      a.id
                FROM
                      Appointment a
                LEFT JOIN a.contemplation c
                LEFT JOIN a.medicalProcedure mp
                LEFT JOIN mp.specialty s
                LEFT JOIN a.patient p
                WHERE p.id = :id AND p.basicHealthUnit.id = :ubsId
                ORDER BY a.requestDate DESC
                """, Long.class);

        appointmentsHistoryIdQueryPaginated.setParameter("id", patient.getId());
        appointmentsHistoryIdQueryPaginated.setParameter("ubsId", patient.getBasicHealthUnit().getId());

        appointmentsHistoryIdQueryPaginated.setFirstResult(page.getPageNumber() * page.getPageSize());
        appointmentsHistoryIdQueryPaginated.setMaxResults(page.getPageSize());

        var appointmentsHistoryIdsPaginated = appointmentsHistoryIdQueryPaginated.getResultList();
        long totalCountAppointmentsHistory = 0;

        if (appointmentsHistoryIdsPaginated.size() < page.getPageSize()) {
            totalCountAppointmentsHistory = appointmentsHistoryIdsPaginated.size();
        } else {
            TypedQuery<Long> count = em.createQuery("""
                SELECT
                      COUNT(a.id)
                FROM
                      Appointment a
                LEFT JOIN a.contemplation c
                LEFT JOIN a.medicalProcedure mp
                LEFT JOIN mp.specialty s
                LEFT JOIN a.patient p
                WHERE p.id = :id AND p.basicHealthUnit.id = :ubsId
                """, Long.class);

            count.setParameter("id", patient.getId());
            count.setParameter("ubsId", patient.getBasicHealthUnit().getId());
            totalCountAppointmentsHistory = (long) count.getSingleResult();
        }

        TypedQuery<PatientAppointmentsHistoryDTO> appointmentsHistoryQuery = em.createQuery("""
                SELECT
                      new br.com.tecsus.sigaubs.dtos.PatientAppointmentsHistoryDTO(
                          a.requestDate,
                          COALESCE(c.contemplationDate, null),
                          COALESCE(c.contemplatedBy, null),
                          a.priority,
                          a.status,
                          mp.procedureType,
                          mp.description,
                          s.title,
                          COALESCE(a.observation, 'Sem observações.'),
                          mp.id,
                          a.id,
                          COALESCE(c.id, null),
                          p.id)
                FROM
                      Appointment a
                LEFT JOIN a.contemplation c
                LEFT JOIN a.medicalProcedure mp
                LEFT JOIN mp.specialty s
                LEFT JOIN a.patient p
                WHERE a.id IN :ids
                ORDER BY a.requestDate DESC
                """, PatientAppointmentsHistoryDTO.class);

        appointmentsHistoryQuery.setParameter("ids", appointmentsHistoryIdsPaginated);
        List<PatientAppointmentsHistoryDTO> patientHistory = appointmentsHistoryQuery.getResultList();

        return new PageImpl<>(patientHistory, page, totalCountAppointmentsHistory);
    }

}
