package br.com.tecsus.sigaubs.entities;

import br.com.tecsus.sigaubs.entities.converters.YearMonthDateAttributeConverter;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Convert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@DynamicUpdate
@Table(name = "medical_slots")
public class MedicalSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @DateTimeFormat(pattern = "yyyy-MM")
    @Convert(converter = YearMonthDateAttributeConverter.class)
    @Column(name = "reference_month", columnDefinition = "date")
    private YearMonth referenceMonth;

    @Column(name = "total_slots")
    private Integer totalSlots;

    @Column(name = "current_slots")
    private Integer currentSlots;

    @OneToMany(mappedBy = "medicalSlot")
    private Set<Contemplation> contemplations = new HashSet<>();

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


    public MedicalSlot() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public YearMonth getReferenceMonth() {
        return referenceMonth;
    }

    public void setReferenceMonth(YearMonth referenceMonth) {
        this.referenceMonth = referenceMonth;
    }

    public Integer getTotalSlots() {
        return totalSlots;
    }

    public void setTotalSlots(Integer totalSlots) {
        this.totalSlots = totalSlots;
    }

    public Integer getCurrentSlots() {
        return currentSlots;
    }

    public void setCurrentSlots(Integer currentSlots) {
        this.currentSlots = currentSlots;
    }

    public Set<Contemplation> getContemplations() {
        return contemplations;
    }

    public void setContemplations(Set<Contemplation> contemplations) {
        this.contemplations = contemplations;
    }

    public MedicalProcedure getMedicalProcedure() {
        return medicalProcedure;
    }

    public void setMedicalProcedure(MedicalProcedure medicalProcedure) {
        this.medicalProcedure = medicalProcedure;
    }

    public BasicHealthUnit getBasicHealthUnit() {
        return basicHealthUnit;
    }

    public void setBasicHealthUnit(BasicHealthUnit basicHealthUnit) {
        this.basicHealthUnit = basicHealthUnit;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MedicalSlot that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
