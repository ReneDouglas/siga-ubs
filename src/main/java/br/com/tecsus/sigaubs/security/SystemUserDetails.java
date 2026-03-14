package br.com.tecsus.sigaubs.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import java.util.Collection;

public class SystemUserDetails extends User {

    private String name;
    private String email;
    private Boolean active;
    private Long basicHealthUnitId;

    public SystemUserDetails(String username,
                             String password,
                             Collection<? extends GrantedAuthority> authorities,
                             String name,
                             String email,
                             Boolean active,
                             Long basicHealthUnitId) {
        super(username, password, authorities);
        this.name = name;
        this.email = email;
        this.active = active;
        this.basicHealthUnitId = basicHealthUnitId;
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

    public Long getBasicHealthUnitId() {
        return basicHealthUnitId;
    }

    public void setBasicHealthUnitId(Long basicHealthUnitId) {
        this.basicHealthUnitId = basicHealthUnitId;
    }
}
