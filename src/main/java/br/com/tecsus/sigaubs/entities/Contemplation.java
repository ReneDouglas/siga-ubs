package br.com.tecsus.sigaubs.entities;

import br.com.tecsus.sigaubs.entities.converters.PriorityConverter;
import br.com.tecsus.sigaubs.enums.Priorities;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Convert;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@DynamicUpdate
@Table(name = "contemplations")
public class Contemplation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contemplation_date")
    private LocalDateTime contemplationDate;

    @Column(name = "contemplated_by")
    @Convert(converter = PriorityConverter.class)
    private Priorities contemplatedBy;

    @OneToOne(mappedBy = "contemplation")
    private Appointment appointment;

    @ManyToOne
    @JoinColumn(name = "id_available_medical_slot")
    private MedicalSlot medicalSlot;

    private String observation;

    @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    @Column(name = "creation_user", updatable = false)
    private String creationUser;

    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @Column(name = "update_user")
    private String updateUser;

    public Contemplation() {
    }

    public boolean isEmptyObservation() {
        return this.getObservation() == null || this.getObservation().isEmpty();
    }
}
