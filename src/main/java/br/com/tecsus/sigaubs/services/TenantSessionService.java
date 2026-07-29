package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.security.SessionMetadata;
import br.com.tecsus.sigaubs.security.SessionPrincipalKey;
import br.com.tecsus.sigaubs.enums.Roles;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.List;
import java.util.UUID;
import br.com.tecsus.sigaubs.dtos.SessionSummaryDTO;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

@Service
public class TenantSessionService {

    private final SessionRegistry sessionRegistry;
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public TenantSessionService(
            SessionRegistry sessionRegistry,
            FindByIndexNameSessionRepository<? extends Session> sessionRepository,
            JdbcTemplate jdbcTemplate) {
        this.sessionRegistry = sessionRegistry;
        this.sessionRepository = sessionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    TenantSessionService(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
        this.sessionRepository = null;
        this.jdbcTemplate = null;
    }

    public void expireTenantSessions(Long tenantId) {
        if (tenantId == null) {
            return;
        }
        if (jdbcTemplate != null) {
            jdbcTemplate.update(
                    "DELETE FROM SPRING_SESSION WHERE PRINCIPAL_NAME LIKE ?",
                    SessionPrincipalKey.forTenantUsersLike(tenantId));
        }
        sessionRegistry.getAllPrincipals().stream()
                .filter(SystemUserDetails.class::isInstance)
                .map(SystemUserDetails.class::cast)
                .filter(principal -> Objects.equals(tenantId, principal.getTenantId()))
                .forEach(this::expireSessions);
    }

    public void expireTenantScopedSessions() {
        if (jdbcTemplate != null) {
            jdbcTemplate.update(
                    "DELETE FROM SPRING_SESSION WHERE PRINCIPAL_NAME LIKE ?",
                    SessionPrincipalKey.forAllTenantUsersLike());
        }
        sessionRegistry.getAllPrincipals().stream()
                .filter(SystemUserDetails.class::isInstance)
                .map(SystemUserDetails.class::cast)
                .filter(principal -> principal.getTenantId() != null)
                .forEach(this::expireSessions);
    }

    public void expireTenantUserSessions(Long tenantId, Long userId) {
        expireByPrincipalKey(SessionPrincipalKey.forTenantUser(tenantId, userId));
    }

    public void expireAdminUserSessions(Long userId) {
        expireByPrincipalKey(SessionPrincipalKey.forAdmin(userId));
    }

    public List<SessionSummaryDTO> listSessions(String principalKey, String currentSessionId) {
        if (sessionRepository == null || principalKey == null) {
            return List.of();
        }
        return sessionRepository.findByPrincipalName(principalKey).values().stream()
                .map(session -> toSummary(session, currentSessionId))
                .sorted(java.util.Comparator.comparing(
                        SessionSummaryDTO::lastAccessedAt).reversed())
                .toList();
    }

    public List<SessionSummaryDTO> listOwnSessions(
            SystemUserDetails loggedUser, String currentSessionId) {
        return listSessions(ownPrincipalKey(loggedUser), currentSessionId);
    }

    public List<SessionSummaryDTO> listAllSessions(String currentSessionId) {
        if (sessionRepository == null || jdbcTemplate == null) {
            return List.of();
        }
        return activePrincipalKeys().stream()
                .flatMap(principalKey -> sessionRepository
                        .findByPrincipalName(principalKey)
                        .values()
                        .stream()
                        .map(session -> toSummary(
                                session,
                                currentSessionId,
                                ownerDescription(session))))
                .sorted(Comparator.comparing(
                        SessionSummaryDTO::lastAccessedAt).reversed())
                .toList();
    }

    public boolean revokeSessionForPrincipal(String principalKey, String managementId) {
        if (sessionRepository == null
                || principalKey == null
                || !isValidManagementId(managementId)) {
            return false;
        }
        for (Session session : sessionRepository.findByPrincipalName(principalKey).values()) {
            if (Objects.equals(
                    managementId,
                    session.getAttribute(SessionMetadata.MANAGEMENT_ID_ATTRIBUTE))) {
                sessionRepository.deleteById(session.getId());
                return true;
            }
        }
        return false;
    }

    public boolean revokeAnySession(String managementId) {
        if (!isValidManagementId(managementId)) {
            return false;
        }
        for (String principalKey : activePrincipalKeys()) {
            if (revokeSessionForPrincipal(principalKey, managementId)) {
                return true;
            }
        }
        return false;
    }

    public boolean revokeOwnSession(
            SystemUserDetails loggedUser, String managementId) {
        return revokeSessionForPrincipal(
                ownPrincipalKey(loggedUser), managementId);
    }

    private void expireByPrincipalKey(String principalKey) {
        if (sessionRepository == null || principalKey == null) {
            return;
        }
        sessionRepository.findByPrincipalName(principalKey)
                .keySet()
                .forEach(sessionRepository::deleteById);
    }

    private void expireSessions(SystemUserDetails principal) {
        sessionRegistry.getAllSessions(principal, false).stream()
                .filter(session -> !session.isExpired())
                .forEach(SessionInformation::expireNow);
    }

    private List<String> activePrincipalKeys() {
        if (jdbcTemplate == null) {
            return List.of();
        }
        return jdbcTemplate.queryForList(
                        """
                        SELECT DISTINCT PRINCIPAL_NAME
                        FROM SPRING_SESSION
                        WHERE PRINCIPAL_NAME IS NOT NULL
                          AND EXPIRY_TIME > ?
                        """,
                        String.class,
                        Instant.now().toEpochMilli())
                .stream()
                .filter(this::isApplicationPrincipalKey)
                .toList();
    }

    private boolean isApplicationPrincipalKey(String principalKey) {
        return SessionPrincipalKey.isApplicationKey(principalKey);
    }

    private SessionSummaryDTO toSummary(Session session, String currentSessionId) {
        return toSummary(session, currentSessionId, null);
    }

    private SessionSummaryDTO toSummary(
            Session session,
            String currentSessionId,
            String ownerDescription) {
        String managementId = session.getAttribute(SessionMetadata.MANAGEMENT_ID_ATTRIBUTE);
        if (!isValidManagementId(managementId)) {
            managementId = SessionMetadata.newManagementId();
            session.setAttribute(SessionMetadata.MANAGEMENT_ID_ATTRIBUTE, managementId);
            saveSessionFromRepository(session);
        }
        String clientDescription = session.getAttribute(SessionMetadata.CLIENT_DESCRIPTION_ATTRIBUTE);
        if (clientDescription == null || clientDescription.isBlank()) {
            clientDescription = SessionMetadata.CLIENT_UNIDENTIFIED;
        }
        String locationDescription =
                session.getAttribute(SessionMetadata.LOCATION_DESCRIPTION_ATTRIBUTE);
        if (locationDescription == null || locationDescription.isBlank()) {
            locationDescription = SessionMetadata.LOCATION_UNAVAILABLE;
        }
        return new SessionSummaryDTO(
                managementId,
                session.getCreationTime(),
                session.getLastAccessedTime(),
                session.getMaxInactiveInterval(),
                clientDescription,
                locationDescription,
                Objects.equals(session.getId(), currentSessionId),
                ownerDescription);
    }

    private String ownerDescription(Session session) {
        Object contextAttribute = session.getAttribute(
                SPRING_SECURITY_CONTEXT_KEY);
        if (!(contextAttribute instanceof SecurityContext securityContext)
                || securityContext.getAuthentication() == null
                || !(securityContext.getAuthentication().getPrincipal()
                        instanceof SystemUserDetails principal)) {
            return "Conta não identificada";
        }

        String role = principal.getAuthorities().stream()
                .map(authority -> authority.getAuthority()
                        .replaceFirst("^ROLE_", ""))
                .sorted()
                .findFirst()
                .orElse("SEM PAPEL");
        String tenant = principal.getTenantSlug() == null
                ? ""
                : " · " + principal.getTenantSlug();
        return principal.getName()
                + " (" + principal.getLoginUsername() + ")"
                + " · " + role
                + tenant;
    }

    private boolean isValidManagementId(String managementId) {
        if (managementId == null) {
            return false;
        }
        try {
            UUID.fromString(managementId);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String ownPrincipalKey(SystemUserDetails loggedUser) {
        if (loggedUser == null || loggedUser.getUserId() == null) {
            return null;
        }
        boolean admin = loggedUser.getAuthorities().stream()
                .anyMatch(authority ->
                        Roles.ROLE_ADMIN.name().equals(authority.getAuthority()));
        if (admin) {
            return SessionPrincipalKey.forAdmin(loggedUser.getUserId());
        }
        if (loggedUser.getTenantId() == null) {
            return null;
        }
        return SessionPrincipalKey.forTenantUser(
                loggedUser.getTenantId(), loggedUser.getUserId());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void saveSessionFromRepository(Session session) {
        // A sessão foi criada pelo próprio repositório; o cast apenas recompõe o
        // tipo concreto perdido pelo wildcard usado para aceitar JdbcSession.
        ((FindByIndexNameSessionRepository) sessionRepository).save(session);
    }
}
