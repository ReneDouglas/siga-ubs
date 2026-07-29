package br.com.tecsus.sigaubs.dtos;

import br.com.tecsus.sigaubs.config.SecurityProperties;
import br.com.tecsus.sigaubs.utils.DocumentValidationUtils;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class SystemUserCommandDTO {

    @Positive
    private Long id;
    @Size(min = 3, max = 100)
    private String username;
    @Size(max = SecurityProperties.Password.MAXIMUM_SUPPORTED_LENGTH)
    private String password;
    @Size(max = SecurityProperties.Password.MAXIMUM_SUPPORTED_LENGTH)
    private String confirmPassword;
    @NotBlank
    @Size(max = 255)
    private String name;
    @NotBlank
    @Email
    @Size(max = 255)
    private String email;
    private Boolean active;
    @Positive
    private Long basicHealthUnit;
    @NotNull
    @Positive
    private Long selectedRoleId;

    @AssertTrue(message = "As senhas não conferem ou não atendem à política.")
    public boolean isPasswordValid() {
        if (id != null && (password == null || password.isBlank())) {
            return confirmPassword == null || confirmPassword.isBlank();
        }
        return password != null
                && password.equals(confirmPassword)
                && DocumentValidationUtils.isAcceptablePassword(
                        password,
                        SecurityProperties.Password.MINIMUM_SUPPORTED_LENGTH,
                        SecurityProperties.Password.MAXIMUM_SUPPORTED_LENGTH);
    }

    @AssertTrue(message = "Login obrigatório.")
    public boolean isUsernameValid() {
        return id != null || (username != null && !username.isBlank());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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
}
