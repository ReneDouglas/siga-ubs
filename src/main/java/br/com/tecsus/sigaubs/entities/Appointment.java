package br.com.tecsus.sigaubs.entities;

import br.com.tecsus.sigaubs.entities.converters.AppointmentStatusConverter;
import br.com.tecsus.sigaubs.entities.converters.PriorityConverter;
import br.com.tecsus.sigaubs.enums.AppointmentStatus;
import br.com.tecsus.sigaubs.enums.Priorities;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@DynamicUpdate
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_date", updatable = false)
    private LocalDateTime requestDate;

    @Convert(converter = PriorityConverter.class)
    private Priorities priority;

    private String observation;

    @Convert(converter = AppointmentStatusConverter.class)
    private AppointmentStatus status;

    @OneToMany(mappedBy = "appointment")
    private List<AppointmentStatusHistory> appointmentStatusHistory = new ArrayList<>();

    @Column(name = "creation_user", updatable = false)
    private String creationUser;

    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @Column(name = "update_user")
    private String updateUser;

    @OneToOne
    @JoinColumn(name = "id_medical_procedure", updatable = false)
    private MedicalProcedure medicalProcedure;

    @OneToOne
    @JoinColumn(name = "id_patient", updatable = false)
    private Patient patient;

    @OneToOne
    @JoinColumn(name = "id_contemplation")
    private Contemplation contemplation;

    public Appointment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public Priorities getPriority() {
        return priority;
    }

    public void setPriority(Priorities priority) {
        this.priority = priority;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public List<AppointmentStatusHistory> getAppointmentStatusHistory() {
        return appointmentStatusHistory;
    }

    public void setAppointmentStatusHistory(List<AppointmentStatusHistory> appointmentStatusHistory) {
        this.appointmentStatusHistory = appointmentStatusHistory;
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

    public MedicalProcedure getMedicalProcedure() {
        return medicalProcedure;
    }

    public void setMedicalProcedure(MedicalProcedure medicalProcedure) {
        this.medicalProcedure = medicalProcedure;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Contemplation getContemplation() {
        return contemplation;
    }

    public void setContemplation(Contemplation contemplation) {
        this.contemplation = contemplation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Appointment that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
