package br.com.tecsus.sccubs.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "available_medical_slots")
public class AvailableMedicalSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Temporal(TemporalType.DATE)
    @Column(name = "reference_month", updatable = false)
    private LocalDate referenceMonth;

    @Column(name = "total_slots")
    private Integer totalSlots;

    @Column(name = "current_slots")
    private Integer currentSlots;

    @OneToOne
    @JoinColumn(name = "id_medical_procedure", updatable = false)
    private MedicalProcedure medicalProcedure;

    @OneToOne
    @JoinColumn(name = "id_basic_health_unit", updatable = false)
    private BasicHealthUnit basicHealthUnit;

    @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    @Column(name = "creation_user", updatable = false)
    private String creationUser;

    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @Column(name = "update_user")
    private String updateUser;

    public AvailableMedicalSlot() {
    }
}
