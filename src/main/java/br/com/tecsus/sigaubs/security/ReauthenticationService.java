package br.com.tecsus.sigaubs.security;

import br.com.tecsus.sigaubs.config.SecurityProperties;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Service
public class ReauthenticationService {

    public static final String MANUAL_CONTEMPLATION = "MANUAL_CONTEMPLATION";
    private static final String SESSION_ATTRIBUTE =
            ReauthenticationService.class.getName() + ".GRANT";

    private final SecurityProperties properties;
    private final Clock clock;

    @Autowired
    public ReauthenticationService(SecurityProperties properties) {
        this(properties, Clock.systemUTC());
    }

    ReauthenticationService(SecurityProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void issue(HttpSession session, SystemUserDetails user, String action, Long objectId) {
        if (!MANUAL_CONTEMPLATION.equals(action) || objectId == null || user == null) {
            throw new IllegalArgumentException("Ação de reautenticação inválida.");
        }
        session.setAttribute(SESSION_ATTRIBUTE, new Grant(
                user.getSessionPrincipalKey(),
                action,
                objectId,
                clock.instant()));
    }

    public void requireAndConsume(
            HttpSession session, SystemUserDetails user, String action, Long objectId) {
        Object stored = session.getAttribute(SESSION_ATTRIBUTE);
        session.removeAttribute(SESSION_ATTRIBUTE);
        if (!(stored instanceof Grant grant)
                || user == null
                || !Objects.equals(grant.principalKey(), user.getSessionPrincipalKey())
                || !Objects.equals(grant.action(), action)
                || !Objects.equals(grant.objectId(), objectId)
                || grant.issuedAt().plus(properties.getSession().getReauthentication())
                        .isBefore(clock.instant())) {
            throw new AccessDeniedException(
                    "Reautenticação recente e vinculada a esta contemplação é obrigatória.");
        }
    }

    private record Grant(
            String principalKey,
            String action,
            Long objectId,
            Instant issuedAt) implements Serializable {
    }
}
