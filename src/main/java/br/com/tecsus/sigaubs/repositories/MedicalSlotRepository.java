package br.com.tecsus.sigaubs.repositories;

import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.MedicalProcedure;
import br.com.tecsus.sigaubs.entities.MedicalSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MedicalSlotRepository extends JpaRepository<MedicalSlot, Long>, MedicalSlotRepositoryCustom {

    @Transactional(readOnly = true)
    MedicalSlot findByMedicalProcedureAndBasicHealthUnitAndContemplationsIsNull(MedicalProcedure medicalProcedure,
            BasicHealthUnit basicHealthUnit);

    @Transactional(readOnly = true)
    @Query("""
                SELECT ms
                FROM MedicalSlot ms
                WHERE ms.medicalProcedure.id = :medicalProcedureId
                AND ms.basicHealthUnit.id = :ubsId
                AND ms.currentSlots > 0
            """)
    Optional<MedicalSlot> findAvailableSlotsByMedicalProcedureAndUBS(Long medicalProcedureId, Long ubsId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE MedicalSlot ms
               SET ms.currentSlots = ms.currentSlots - 1,
                   ms.version = ms.version + 1
             WHERE ms.id = :id
               AND ms.currentSlots > 0
            """)
    int decrementIfAvailable(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE MedicalSlot ms
               SET ms.currentSlots = ms.currentSlots + 1,
                   ms.version = ms.version + 1
             WHERE ms.id = :id
               AND ms.currentSlots < ms.totalSlots
            """)
    int incrementIfBelowTotal(@Param("id") Long id);

    @Transactional(readOnly = true)
    @Query("""
                SELECT ms
                FROM MedicalSlot ms
                LEFT JOIN FETCH ms.medicalProcedure
                LEFT JOIN FETCH ms.basicHealthUnit
                WHERE ms.referenceMonth >= :startOfMonth
                AND ms.referenceMonth < :startOfNextMonth
                AND ms.currentSlots > 0
            """)
    List<MedicalSlot> findAllAvailableSlotsByReferenceMonth(
            @Param("startOfMonth") LocalDate startOfMonth,
            @Param("startOfNextMonth") LocalDate startOfNextMonth);

}
