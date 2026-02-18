package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.entities.SystemUser;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.services.BasicHealthUnitService;
import br.com.tecsus.sigaubs.services.DashboardService;
import br.com.tecsus.sigaubs.services.SystemUserService;
import br.com.tecsus.sigaubs.services.exceptions.InvalidConfirmPasswordException;
import br.com.tecsus.sigaubs.utils.DefaultValues;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@SessionScope
public class SessionController {

    private final SystemUserService systemUserService;
    private final BasicHealthUnitService basicHealthUnitService;
    private final DashboardService dashboardService;

    @Autowired
    public SessionController(SystemUserService systemUserService, BasicHealthUnitService basicHealthUnitService,
            DashboardService dashboardService) {
        this.systemUserService = systemUserService;
        this.basicHealthUnitService = basicHealthUnitService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String getHomePage(Model model, @AuthenticationPrincipal SystemUserDetails loggedUser) {
        boolean isAdminOrSms = loggedUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SMS"));

        if (isAdminOrSms) {
            model.addAttribute("dashboard", dashboardService.loadDashboardData());
        } else {
            Long ubsId = loggedUser.getBasicHealthUnitId();
            if (ubsId != null) {
                model.addAttribute("ubsDashboard", dashboardService.loadUBSDashboardData(ubsId));
            }
        }
        return "home";
    }

    @GetMapping("/login")
    public String getLoginPage() {
        return "sessionManagement/login";
    }

    @GetMapping("/logout")
    public String getLogoutPage() {
        return "sessionManagement/login";
    }

    @GetMapping("/login-error")
    public String getLoginErrorPage(Model model) {
        model.addAttribute("loginError", true);
        return "sessionManagement/login";
    }

    @GetMapping("/error")
    public String getErrorPage() {
        return "error";
    }

    @GetMapping("/expired")
    public String getExpiredPage() {
        return "expired";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @GetMapping("/systemUser-management")
    public String getSystemUserInsertPage(Model model,
            @ModelAttribute("searchUser") SystemUser systemUser,
            @RequestParam(value = "page", defaultValue = "0", required = false) int currentPage,
            @RequestParam(value = "size", defaultValue = "" + DefaultValues.PAGE_SIZE, required = false) int pageSize,
            HttpServletRequest request) {

        Page<SystemUser> systemUsersPage;

        model.addAttribute("systemUser", new SystemUser());
        model.addAttribute("rolesList", systemUserService.getRolesNotAdminAndNotManagement());
        model.addAttribute("basicHealthUnits", basicHealthUnitService
                .findAllUBS());

        systemUser.setCreationUser(SecurityContextHolder.getContext().getAuthentication().getName());
        systemUser
                .setName(systemUser.getName() == null || systemUser.getName().isEmpty() ? null : systemUser.getName());
        systemUser.setUsername(systemUser.getUsername() == null || systemUser.getUsername().isEmpty() ? null
                : systemUser.getUsername());

        systemUsersPage = systemUserService
                .findAllUsersByCreationUserPaginated(systemUser,
                        PageRequest.of(currentPage, pageSize, Sort.Direction.valueOf("DESC"), "creationDate"));
        model.addAttribute("systemUsersPage", systemUsersPage);

        if ("searchRequest".equals(request.getHeader("X-Requested-With"))) {
            return "sessionManagement/sessionFragments/systemUserList-datatable :: systemUser-datatable";
        } else {
            return "sessionManagement/systemUser-management";
        }

    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @PostMapping("/systemUser-management/create")
    public String registerSystemUser(@ModelAttribute SystemUser systemUser,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {

        try {
            systemUserService.registerNotAdminSystemUser(systemUser, loggedUser);
            redirectAttributes.addFlashAttribute("message", "Usuário cadastrado com sucesso.");
            redirectAttributes.addFlashAttribute("error", false);
            log.info("Cadastro de usuário realizado com sucesso.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("message", "Usuário já cadastrado no sistema.");
            redirectAttributes.addFlashAttribute("error", true);
            log.error("Usuário já cadastrado: {}", e.getMessage());
        } catch (InvalidConfirmPasswordException e) {
            redirectAttributes.addFlashAttribute("message", "As senhas não conferem.");
            redirectAttributes.addFlashAttribute("error", true);
            log.error(e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Erro ao cadastrar usuário.");
            redirectAttributes.addFlashAttribute("error", true);
            log.error("Erro ao cadastrar usuário: {}", e.getMessage());
        }
        return "redirect:/systemUser-management";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @PostMapping("/systemUser-management")
    public String getSystemUserInsertPageToUpdate(@RequestParam("id") Long id,
            Model model,
            @AuthenticationPrincipal SystemUserDetails loggedUser) {

        model.addAttribute("systemUser", systemUserService.findSystemUserById(id));
        model.addAttribute("searchUser", new SystemUser());
        model.addAttribute("rolesList", systemUserService.getRolesNotAdminAndNotManagement());
        model.addAttribute("basicHealthUnits", basicHealthUnitService
                .findAllUBS());

        SystemUser su = new SystemUser();
        su.setCreationUser(loggedUser.getUsername());

        Page<SystemUser> systemUsersPage = systemUserService
                .findAllUsersByCreationUserPaginated(su,
                        PageRequest.of(0, DefaultValues.PAGE_SIZE, Sort.Direction.valueOf("DESC"), "creationDate"));
        model.addAttribute("systemUsersPage", systemUsersPage);

        return "sessionManagement/systemUser-management";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @PostMapping("/systemUser-management/update")
    public String updateSystemUser(@ModelAttribute SystemUser systemUser,
            RedirectAttributes redirectAttributes) {

        try {
            systemUserService.updateNotAdminSystemUser(systemUser);
            redirectAttributes.addFlashAttribute("message", "Usuário atualizado com sucesso.");
            redirectAttributes.addFlashAttribute("error", false);
            log.info("Usuário atualizado com sucesso.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Erro ao atualizar usuário.");
            redirectAttributes.addFlashAttribute("error", true);
            log.error("Erro ao atualizar usuário: {}", e.getMessage());
        }
        // return new RedirectView("/user");
        return "redirect:/systemUser-management";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @PostMapping("/systemUser-management/delete")
    public String deleteSystemUser(@RequestParam("id") Long id, RedirectAttributes redirectAttributes) {

        try {
            systemUserService.deleteNotAdminSystemUser(id);
            redirectAttributes.addFlashAttribute("message", "Usuário deletado com sucesso.");
            redirectAttributes.addFlashAttribute("error", false);
            log.info("Usuário deletado com sucesso.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Erro ao deletar usuário.");
            redirectAttributes.addFlashAttribute("error", true);
            log.error("Erro ao deletar usuário: {}", e.getMessage());
        }

        return "redirect:/systemUser-management";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @PostMapping("/systemUser-management/validate")
    public ResponseEntity<String> validateSystemUserByPassword(@RequestParam String password,
            @AuthenticationPrincipal SystemUserDetails loggedUser) {

        try {
            log.info("Validando senha do usuário: {}", loggedUser.getName());
            boolean isValid = systemUserService.validateSystemUserByPassword(password, loggedUser);
            if (isValid) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.badRequest()
                        .body("Senha inválida");
            }
        } catch (Exception e) {
            log.error("Erro ao validar senha: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao validar senha.");
        }
    }

}
