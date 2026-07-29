package br.com.tecsus.sigaubs.dtos;

import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.Patient;
import br.com.tecsus.sigaubs.enums.SocialSituationRating;
import jakarta.validation.constraints.Size;

public class PatientSearchDTO {

    @Size(max = 255)
    private String name;
    @Size(max = 14)
    private String cpf;
    @Size(max = 18)
    private String susNumber;
    @Size(max = 255)
    private String acsName;
    @Size(max = 255)
    private String addressStreet;
    private SocialSituationRating socialSituationRating;
    private Long basicHealthUnit;

    public Patient toFilterEntity() {
        Patient patient = new Patient();
        patient.setName(blankToNull(name));
        patient.setCpf(blankToNull(cpf));
        patient.setSusNumber(blankToNull(susNumber));
        patient.setAcsName(blankToNull(acsName));
        patient.setAddressStreet(blankToNull(addressStreet));
        patient.setSocialSituationRating(socialSituationRating);
        if (basicHealthUnit != null) {
            BasicHealthUnit unit = new BasicHealthUnit();
            unit.setId(basicHealthUnit);
            patient.setBasicHealthUnit(unit);
        }
        return patient;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getSusNumber() {
        return susNumber;
    }

    public void setSusNumber(String susNumber) {
        this.susNumber = susNumber;
    }

    public String getAcsName() {
        return acsName;
    }

    public void setAcsName(String acsName) {
        this.acsName = acsName;
    }

    public String getAddressStreet() {
        return addressStreet;
    }

    public void setAddressStreet(String addressStreet) {
        this.addressStreet = addressStreet;
    }

    public SocialSituationRating getSocialSituationRating() {
        return socialSituationRating;
    }

    public void setSocialSituationRating(SocialSituationRating socialSituationRating) {
        this.socialSituationRating = socialSituationRating;
    }

    public Long getBasicHealthUnit() {
        return basicHealthUnit;
    }

    public void setBasicHealthUnit(Long basicHealthUnit) {
        this.basicHealthUnit = basicHealthUnit;
    }
}
