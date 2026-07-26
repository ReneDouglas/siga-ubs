package br.com.tecsus.sigaubs.repositories;

import br.com.tecsus.sigaubs.dtos.MedicalProceduresTotalDTO;
import br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO;
import br.com.tecsus.sigaubs.dtos.ProcedureTypeTotalDTO;
import br.com.tecsus.sigaubs.entities.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, AppointmentRepositoryCustom {

    @Transactional(readOnly = true)
    @Query("""
        SELECT a
        FROM Appointment a
        JOIN FETCH a.patient p
        LEFT JOIN FETCH p.basicHealthUnit
        JOIN FETCH a.medicalProcedure mp
        LEFT JOIN FETCH mp.specialty
        WHERE a.id = :id
    """)
    Optional<Appointment> findByIdWithQueueDetails(@Param("id") Long id);

    @Transactional(readOnly = true)
    @Query("""
        SELECT
            new br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO(
                a.requestDate,
                a.priority,
                mp.procedureType,
                mp.id,
                mp.description,
                s.title,
                null,
                COALESCE(a.observation, "Sem observações."),
                a.id,
                a.patient.id,
                null,
                null,
                null,
                null,
                null)
        FROM
            Appointment a
        LEFT JOIN a.contemplation c
        LEFT JOIN a.medicalProcedure mp
        LEFT JOIN mp.specialty s
        WHERE
            a.patient.id = :id
        AND a.contemplation IS NULL
        AND a.status = br.com.tecsus.sigaubs.enums.AppointmentStatus.AGUARDANDO_CONTEMPLACAO
        ORDER BY a.requestDate DESC
    """)
    List<PatientOpenAppointmentDTO> findPatientOpenAppointments(@Param("id") Long id);


    @Transactional(readOnly = true)
    @Query("""
        SELECT
            new br.com.tecsus.sigaubs.dtos.ProcedureTypeTotalDTO(
                mp.procedureType,
                COUNT(mp.id))
        FROM
            Appointment a
        LEFT JOIN a.medicalProcedure mp
        LEFT JOIN mp.specialty s
        LEFT JOIN a.patient p
        LEFT JOIN p.basicHealthUnit ubs
        WHERE
            ubs.id = :basicHealthUnit
            AND s.id = :specialty
        GROUP BY mp.procedureType
    """)
    List<ProcedureTypeTotalDTO> totalByProcedureTypeAndUBSAndSpecialty(Long basicHealthUnit, Long specialty);


    @Transactional(readOnly = true)
    @Query("""
        SELECT
            new br.com.tecsus.sigaubs.dtos.MedicalProceduresTotalDTO(
                mp.description,
                mp.procedureType,
                COUNT(mp.id))
        FROM
            Appointment a
        LEFT JOIN a.medicalProcedure mp
        LEFT JOIN mp.specialty s
        LEFT JOIN a.patient p
        LEFT JOIN p.basicHealthUnit ubs
        WHERE
            ubs.id = :basicHealthUnit
            AND s.id = :specialty
            AND mp.procedureType <> br.com.tecsus.sigaubs.enums.ProcedureType.CONSULTA
        GROUP BY mp.id
    """)
    List<MedicalProceduresTotalDTO> totalByMedicalProceduresAndUBSAndSpecialty(Long basicHealthUnit, Long specialty);

}
