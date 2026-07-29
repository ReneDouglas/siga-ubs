package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.entities.SystemAdmin;
import br.com.tecsus.sigaubs.entities.SystemUser;
import br.com.tecsus.sigaubs.exceptions.ResourceNotFoundException;
import br.com.tecsus.sigaubs.security.SessionMetadata;
import br.com.tecsus.sigaubs.security.SessionPrincipalKey;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.services.AdminSmsUserService;
import br.com.tecsus.sigaubs.services.AdminUserManagementService;
import br.com.tecsus.sigaubs.services.SystemUserService;
import br.com.tecsus.sigaubs.services.TenantSessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;

@Controller
public class SessionManagementController {

    private final TenantSessionService tenantSessionService;
    private final SystemUserService systemUserService;
    private final AdminSmsUserService adminSmsUserService;
    private final AdminUserManagementService adminUserManagementService;

    public SessionManagementController(
            TenantSessionService tenantSessionService,
            SystemUserService systemUserService,
            AdminSmsUserService adminSmsUserService,
            AdminUserManagementService adminUserManagementService) {
        this.tenantSessionService = tenantSessionService;
        this.systemUserService = systemUserService;
        this.adminSmsUserService = adminSmsUserService;
        this.adminUserManagementService = adminUserManagementService;
    }

    @GetMapping("/session-management")
    public String ownTenantSessions(
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            HttpSession currentSession,
            Model model) {
        return renderOwnPage(
                loggedUser,
                currentSession.getId(),
                loggedUser.getName(),
                "/session-management",
                "/",
                false,
                true,
                model);
    }

    @PostMapping("/session-management/{managementId}/revoke")
    public String revokeOwnTenantSession(
            @PathVariable String managementId,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            HttpSession currentSession,
            RedirectAttributes redirectAttributes) {
        if (revokeCurrentIfMatches(managementId, currentSession)) {
            addRevocationResult(true, redirectAttributes);
        } else {
            addRevocationResult(
                    tenantSessionService.revokeOwnSession(
                            loggedUser, managementId),
                    redirectAttributes);
        }
        return "redirect:/session-management";
    }

    @PreAuthorize("hasRole('SMS')")
    @GetMapping("/systemUser-management/{userId}/sessions")
    public String managedTenantUserSessions(
            @PathVariable Long userId,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            Model model) {
        SystemUser target = requireManageableTenantUser(userId, loggedUser);
        return renderPage(
                SessionPrincipalKey.forTenantUser(
                        loggedUser.getTenantId(), target.getId()),
                null,
                target.getName(),
                "/systemUser-management/" + target.getId() + "/sessions",
                "/systemUser-management",
                false,
                false,
                model);
    }

    @PreAuthorize("hasRole('SMS')")
    @PostMapping("/systemUser-management/{userId}/sessions/{managementId}/revoke")
    public String revokeManagedTenantUserSession(
            @PathVariable Long userId,
            @PathVariable String managementId,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {
        SystemUser target = requireManageableTenantUser(userId, loggedUser);
        revoke(
                SessionPrincipalKey.forTenantUser(
                        loggedUser.getTenantId(), target.getId()),
                managementId,
                redirectAttributes);
        return "redirect:/systemUser-management/" + target.getId() + "/sessions";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/session-management")
    public String activeAdminSessions(
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            HttpSession currentSession,
            Model model) {
        model.addAttribute(
                "sessions",
                tenantSessionService.listAllSessions(currentSession.getId()));
        model.addAttribute("targetName", loggedUser.getName());
        model.addAttribute("revokeBaseUrl", "/admin/session-management");
        model.addAttribute("backUrl", "/admin/tenant-management");
        model.addAttribute("adminArea", true);
        model.addAttribute("ownSessions", false);
        model.addAttribute("globalSessions", true);
        return "sessionManagement/session_management";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/session-management/{managementId}/revoke")
    public String revokeAdminSession(
            @PathVariable String managementId,
            HttpSession currentSession,
            RedirectAttributes redirectAttributes) {
        if (revokeCurrentIfMatches(managementId, currentSession)) {
            addRevocationResult(true, redirectAttributes);
        } else {
            addRevocationResult(
                    tenantSessionService.revokeAnySession(managementId),
                    redirectAttributes);
        }
        return "redirect:/admin/session-management";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/admin-user-management/{adminId}/sessions")
    public String managedAdminSessions(
            @PathVariable Long adminId,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            HttpSession currentSession,
            Model model) {
        SystemAdmin target = requireAdmin(adminId);
        return renderPage(
                SessionPrincipalKey.forAdmin(target.getId()),
                Objects.equals(target.getId(), loggedUser.getUserId())
                        ? currentSession.getId()
                        : null,
                target.getName(),
                "/admin/admin-user-management/" + target.getId() + "/sessions",
                "/admin/admin-user-management",
                true,
                Objects.equals(target.getId(), loggedUser.getUserId()),
                model);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/admin-user-management/{adminId}/sessions/{managementId}/revoke")
    public String revokeManagedAdminSession(
            @PathVariable Long adminId,
            @PathVariable String managementId,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            HttpSession currentSession,
            RedirectAttributes redirectAttributes) {
        SystemAdmin target = requireAdmin(adminId);
        if (Objects.equals(target.getId(), loggedUser.getUserId())
                && revokeCurrentIfMatches(managementId, currentSession)) {
            addRevocationResult(true, redirectAttributes);
        } else {
            revoke(
                    SessionPrincipalKey.forAdmin(target.getId()),
                    managementId,
                    redirectAttributes);
        }
        return "redirect:/admin/admin-user-management/" + target.getId() + "/sessions";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/tenant-management/{tenantId}/sms-users/{userId}/sessions")
    public String managedSmsSessions(
            @PathVariable Long tenantId,
            @PathVariable Long userId,
            Model model) {
        SystemUser target = requireSmsUser(tenantId, userId);
        return renderPage(
                SessionPrincipalKey.forTenantUser(tenantId, target.getId()),
                null,
                target.getName(),
                "/admin/tenant-management/" + tenantId
                        + "/sms-users/" + target.getId() + "/sessions",
                "/admin/tenant-management/" + tenantId + "/sms-users",
                true,
                false,
                model);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/tenant-management/{tenantId}/sms-users/{userId}/sessions/{managementId}/revoke")
    public String revokeManagedSmsSession(
            @PathVariable Long tenantId,
            @PathVariable Long userId,
            @PathVariable String managementId,
            RedirectAttributes redirectAttributes) {
        SystemUser target = requireSmsUser(tenantId, userId);
        revoke(
                SessionPrincipalKey.forTenantUser(tenantId, target.getId()),
                managementId,
                redirectAttributes);
        return "redirect:/admin/tenant-management/" + tenantId
                + "/sms-users/" + target.getId() + "/sessions";
    }

    private String renderPage(
            String principalKey,
            String currentSessionId,
            String targetName,
            String revokeBaseUrl,
            String backUrl,
            boolean adminArea,
            boolean ownSessions,
            Model model) {
        model.addAttribute(
                "sessions",
                tenantSessionService.listSessions(principalKey, currentSessionId));
        model.addAttribute("targetName", targetName);
        model.addAttribute("revokeBaseUrl", revokeBaseUrl);
        model.addAttribute("backUrl", backUrl);
        model.addAttribute("adminArea", adminArea);
        model.addAttribute("ownSessions", ownSessions);
        return "sessionManagement/session_management";
    }

    private String renderOwnPage(
            SystemUserDetails loggedUser,
            String currentSessionId,
            String targetName,
            String revokeBaseUrl,
            String backUrl,
            boolean adminArea,
            boolean ownSessions,
            Model model) {
        model.addAttribute(
                "sessions",
                tenantSessionService.listOwnSessions(
                        loggedUser, currentSessionId));
        model.addAttribute("targetName", targetName);
        model.addAttribute("revokeBaseUrl", revokeBaseUrl);
        model.addAttribute("backUrl", backUrl);
        model.addAttribute("adminArea", adminArea);
        model.addAttribute("ownSessions", ownSessions);
        return "sessionManagement/session_management";
    }

    private boolean revoke(
            String principalKey,
            String managementId,
            RedirectAttributes redirectAttributes) {
        boolean revoked = tenantSessionService.revokeSessionForPrincipal(
                principalKey, managementId);
        addRevocationResult(revoked, redirectAttributes);
        return revoked;
    }

    private void addRevocationResult(
            boolean revoked,
            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(
                "message",
                revoked ? "Sessão encerrada com sucesso." : "Sessão não encontrada.");
        redirectAttributes.addFlashAttribute("error", !revoked);
    }

    private boolean revokeCurrentIfMatches(
            String managementId,
            HttpSession currentSession) {
        if (Objects.equals(
                managementId,
                currentSession.getAttribute(SessionMetadata.MANAGEMENT_ID_ATTRIBUTE))) {
            currentSession.invalidate();
            return true;
        }
        return false;
    }

    private SystemUser requireManageableTenantUser(
            Long userId,
            SystemUserDetails loggedUser) {
        SystemUser target = systemUserService.findManageableSystemUserById(
                userId, loggedUser);
        if (target == null) {
            throw new ResourceNotFoundException("Usuário não encontrado.");
        }
        return target;
    }

    private SystemAdmin requireAdmin(Long adminId) {
        var result = adminUserManagementService.findById(adminId);
        if (result.falhou()) {
            throw new ResourceNotFoundException("Administrador não encontrado.");
        }
        return result.valor();
    }

    private SystemUser requireSmsUser(Long tenantId, Long userId) {
        try {
            return adminSmsUserService.findSmsUser(tenantId, userId);
        } catch (IllegalArgumentException exception) {
            throw new ResourceNotFoundException("Usuário SMS não encontrado.");
        }
    }

}
