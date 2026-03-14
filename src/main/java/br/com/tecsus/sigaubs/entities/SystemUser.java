package br.com.tecsus.sigaubs.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


@Entity
@Table(name = "system_users")
@NamedEntityGraph(name = "SystemUserGraph",
        attributeNodes =
                {       // Adicione relacionamentos que devem ser inicializados com esta entidade
                        @NamedAttributeNode("roles"),
                        @NamedAttributeNode("basicHealthUnit")
                }
)
public class SystemUser implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, updatable=false)
    private String username;
    private String password;
    @Transient private String confirmPassword;
    private String name;
    private String email;
    private Boolean active;

    @ManyToOne
    @JoinColumn(name = "id_basic_health_unit")
    private BasicHealthUnit basicHealthUnit;

    @ManyToMany
    @JoinTable(name = "system_users_roles",
            joinColumns = @JoinColumn(name = "id_system_user"),
            inverseJoinColumns = @JoinColumn(name = "id_system_role"))
    private Set<SystemRole> roles = new HashSet<>();

    @Column(name = "creation_date", updatable = false)
    private LocalDateTime creationDate;

    @Column(name = "creation_user", updatable = false)
    private String creationUser;

    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @Column(name = "update_user")
    private String updateUser;

    @Transient
    private Long selectedRoleId;

    public SystemUser(){
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username == null || username.isEmpty() ? null : username;
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
        return name == null || name.isEmpty() ? null : name;
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

    public BasicHealthUnit getBasicHealthUnit() {
        return basicHealthUnit;
    }

    public void setBasicHealthUnit(BasicHealthUnit basicHealthUnit) {
        this.basicHealthUnit = basicHealthUnit;
    }

    public Set<SystemRole> getRoles() {
        return roles;
    }

    public void setRoles(Set<SystemRole> roles) {
        this.roles = roles;
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

    public Long getSelectedRoleId() {
        return selectedRoleId;
    }

    public void setSelectedRoleId(Long selectedRoleId) {
        this.selectedRoleId = selectedRoleId;
    }

    public Boolean findRole(Long id) {
        for (SystemRole systemRole : roles) {
            if (Objects.equals(systemRole.getId(), id)) {
                return true;
            }
        }
        return false;
    }

    public Long getFirstRole() {
        return roles.stream().iterator().next().getId();
    }

    public String getFirstRoleTitle() {
        return roles.stream().iterator().next().getTitle();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SystemUser that)) return false;
        return username != null && Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
