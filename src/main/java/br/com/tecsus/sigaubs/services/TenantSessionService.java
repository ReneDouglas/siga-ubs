package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.security.SystemUserDetails;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class TenantSessionService {

    private final SessionRegistry sessionRegistry;

    public TenantSessionService(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public void expireTenantSessions(Long tenantId) {
        if (tenantId == null) {
            return;
        }
        sessionRegistry.getAllPrincipals().stream()
                .filter(SystemUserDetails.class::isInstance)
                .map(SystemUserDetails.class::cast)
                .filter(principal -> Objects.equals(tenantId, principal.getTenantId()))
                .forEach(this::expireSessions);
    }

    public void expireTenantScopedSessions() {
        sessionRegistry.getAllPrincipals().stream()
                .filter(SystemUserDetails.class::isInstance)
                .map(SystemUserDetails.class::cast)
                .filter(principal -> principal.getTenantId() != null)
                .forEach(this::expireSessions);
    }

    public void expireTenantUserSessions(Long tenantId, String username) {
        if (tenantId == null || username == null) {
            return;
        }
        sessionRegistry.getAllPrincipals().stream()
                .filter(SystemUserDetails.class::isInstance)
                .map(SystemUserDetails.class::cast)
                .filter(principal -> Objects.equals(tenantId, principal.getTenantId()))
                .filter(principal -> username.equals(principal.getUsername()))
                .forEach(this::expireSessions);
    }

    public void expireAdminUserSessions(String username) {
        if (username == null) {
            return;
        }
        sessionRegistry.getAllPrincipals().stream()
                .filter(SystemUserDetails.class::isInstance)
                .map(SystemUserDetails.class::cast)
                .filter(principal -> principal.getTenantId() == null)
                .filter(principal -> username.equals(principal.getUsername()))
                .forEach(this::expireSessions);
    }

    private void expireSessions(SystemUserDetails principal) {
        sessionRegistry.getAllSessions(principal, false).stream()
                .filter(session -> !session.isExpired())
                .forEach(SessionInformation::expireNow);
    }
}
