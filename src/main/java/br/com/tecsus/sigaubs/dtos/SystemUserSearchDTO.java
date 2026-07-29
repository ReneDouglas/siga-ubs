package br.com.tecsus.sigaubs.dtos;

import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.SystemUser;
import jakarta.validation.constraints.Size;

public class SystemUserSearchDTO {

    @Size(max = 100)
    private String username;
    @Size(max = 255)
    private String name;
    private Long basicHealthUnit;
    private Long selectedRoleId;
    private Boolean active;

    public SystemUser toFilterEntity() {
        SystemUser user = new SystemUser();
        user.setUsername(blankToNull(username));
        user.setName(blankToNull(name));
        user.setSelectedRoleId(selectedRoleId);
        user.setActive(active);
        if (basicHealthUnit != null) {
            BasicHealthUnit unit = new BasicHealthUnit();
            unit.setId(basicHealthUnit);
            user.setBasicHealthUnit(unit);
        }
        return user;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getBasicHealthUnit() {
        return basicHealthUnit;
    }

    public void setBasicHealthUnit(Long basicHealthUnit) {
        this.basicHealthUnit = basicHealthUnit;
    }

    public Long getSelectedRoleId() {
        return selectedRoleId;
    }

    public void setSelectedRoleId(Long selectedRoleId) {
        this.selectedRoleId = selectedRoleId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
