package br.com.tecsus.sigaubs.dtos;

import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.Patient;
import br.com.tecsus.sigaubs.enums.SocialSituationRating;
import br.com.tecsus.sigaubs.utils.DocumentValidationUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class PatientCommandDTO {

    @Positive
    private Long id;

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    private String susNumber;

    @NotBlank
    private String cpf;

    @NotBlank
    @Size(max = 20)
    private String gender;

    @NotNull
    @PastOrPresent
    private LocalDate birthDate;

    @NotNull
    private SocialSituationRating socialSituationRating;

    @NotBlank
    private String phoneNumber;

    @NotBlank
    @Size(max = 255)
    private String addressStreet;

    @Size(max = 20)
    private String addressNumber;

    @Size(max = 255)
    private String addressComplement;

    @Size(max = 255)
    private String addressReference;

    @Size(max = 255)
    private String acsName;

    private EntityIdDTO basicHealthUnit = new EntityIdDTO();

    @AssertTrue(message = "CPF inválido.")
    public boolean isCpfValid() {
        return DocumentValidationUtils.isValidCpf(cpf);
    }

    @AssertTrue(message = "CNS inválido.")
    public boolean isSusNumberValid() {
        return DocumentValidationUtils.isValidCns(susNumber);
    }

    @AssertTrue(message = "Telefone inválido.")
    public boolean isPhoneNumberValid() {
        return DocumentValidationUtils.isValidBrazilianPhone(phoneNumber);
    }

    public Patient toNewPatient() {
        Patient patient = new Patient();
        applyEditableFields(patient);
        patient.setCpf(DocumentValidationUtils.digitsOnly(cpf));
        patient.setSusNumber(DocumentValidationUtils.digitsOnly(susNumber));
        return patient;
    }

    public void applyEditableFields(Patient patient) {
        patient.setName(trim(name));
        patient.setGender(trim(gender));
        patient.setBirthDate(birthDate);
        patient.setSocialSituationRating(socialSituationRating);
        patient.setPhoneNumber(DocumentValidationUtils.digitsOnly(phoneNumber));
        patient.setAddressStreet(trim(addressStreet));
        patient.setAddressNumber(trim(addressNumber));
        patient.setAddressComplement(trim(addressComplement));
        patient.setAddressReference(trim(addressReference));
        patient.setAcsName(trim(acsName));
    }

    public Patient toFormPatient() {
        Patient patient = toNewPatient();
        patient.setId(id);
        if (basicHealthUnit != null && basicHealthUnit.getId() != null) {
            BasicHealthUnit unit = new BasicHealthUnit();
            unit.setId(basicHealthUnit.getId());
            patient.setBasicHealthUnit(unit);
        }
        return patient;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSusNumber() {
        return susNumber;
    }

    public void setSusNumber(String susNumber) {
        this.susNumber = susNumber;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public SocialSituationRating getSocialSituationRating() {
        return socialSituationRating;
    }

    public void setSocialSituationRating(SocialSituationRating socialSituationRating) {
        this.socialSituationRating = socialSituationRating;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddressStreet() {
        return addressStreet;
    }

    public void setAddressStreet(String addressStreet) {
        this.addressStreet = addressStreet;
    }

    public String getAddressNumber() {
        return addressNumber;
    }

    public void setAddressNumber(String addressNumber) {
        this.addressNumber = addressNumber;
    }

    public String getAddressComplement() {
        return addressComplement;
    }

    public void setAddressComplement(String addressComplement) {
        this.addressComplement = addressComplement;
    }

    public String getAddressReference() {
        return addressReference;
    }

    public void setAddressReference(String addressReference) {
        this.addressReference = addressReference;
    }

    public String getAcsName() {
        return acsName;
    }

    public void setAcsName(String acsName) {
        this.acsName = acsName;
    }

    public EntityIdDTO getBasicHealthUnit() {
        return basicHealthUnit;
    }

    public void setBasicHealthUnit(EntityIdDTO basicHealthUnit) {
        this.basicHealthUnit = basicHealthUnit;
    }
}
