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
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getContemplationDate() {
        return contemplationDate;
    }

    public void setContemplationDate(LocalDateTime contemplationDate) {
        this.contemplationDate = contemplationDate;
    }

    public Priorities getContemplatedBy() {
        return contemplatedBy;
    }

    public void setContemplatedBy(Priorities contemplatedBy) {
        this.contemplatedBy = contemplatedBy;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public MedicalSlot getMedicalSlot() {
        return medicalSlot;
    }

    public void setMedicalSlot(MedicalSlot medicalSlot) {
        this.medicalSlot = medicalSlot;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public String getCreationUser() {
        return creationUser;
    }

    public void setCreationUser(String creationUser) {
        this.creationUser = creationUser;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public boolean isEmptyObservation() {
        return this.getObservation() == null || this.getObservation().isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Contemplation that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
