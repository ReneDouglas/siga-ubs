package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.dtos.AdminUserSearchDTO;
import br.com.tecsus.sigaubs.entities.SystemAdmin;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.services.AdminUserManagementService;
import br.com.tecsus.sigaubs.utils.DefaultValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
@RequestMapping("/admin/admin-user-management")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserManagementController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserManagementController.class);
    private static final Set<String> ALLOWED_SORTS = Set.of("username", "name", "email", "active", "creationDate", "updateDate");

    private final AdminUserManagementService adminUserManagementService;

    public AdminUserManagementController(AdminUserManagementService adminUserManagementService) {
        this.adminUserManagementService = adminUserManagementService;
    }

    @GetMapping
    public String getAdminUsersPage(Model model,
            @ModelAttribute("searchAdmin") AdminUserSearchDTO searchAdmin,
            @RequestParam(value = "adminId", required = false) Long adminId,
            @RequestParam(value = "page", defaultValue = "0", required = false) int currentPage,
            @RequestParam(value = "pageSize", defaultValue = "" + DefaultValues.PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sort", defaultValue = "creationDate", required = false) String sort,
            @RequestParam(value = "direction", defaultValue = "DESC", required = false) String direction,
            @RequestParam(value = "pagination", defaultValue = "false", required = false) boolean pagination,
            @AuthenticationPrincipal SystemUserDetails loggedUser) {

        String sortProperty = normalizeSort(sort);
        Sort.Direction sortDirection = normalizeDirection(direction);
        model.addAttribute("adminsPage", adminUserManagementService.findAdmins(
                searchAdmin,
                PageRequest.of(currentPage, pageSize, sortDirection, sortProperty)));
        model.addAttribute("selectedSort", sortProperty);
        model.addAttribute("selectedDirection", sortDirection.name());
        model.addAttribute("currentUsername", loggedUser.getUsername());

        if (pagination) {
            return "adminUserManagement/adminUserFragments/admin_user_datatable";
        }

        model.addAttribute("systemAdmin", adminId != null
                ? adminUserManagementService.findById(adminId)
                : new SystemAdmin());
        return "adminUserManagement/admin_user_management";
    }

    @PostMapping("/create")
    public String createAdmin(@ModelAttribute SystemAdmin systemAdmin,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {
        try {
            adminUserManagementService.create(systemAdmin, loggedUser);
            redirectAttributes.addFlashAttribute("message", "Administrador cadastrado com sucesso.");
            redirectAttributes.addFlashAttribute("error", false);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("error", true);
            log.error("Erro ao cadastrar administrador: {}", e.getMessage());
        }
        return "redirect:/admin/admin-user-management";
    }

    @PostMapping("/update")
    public String updateAdmin(@ModelAttribute SystemAdmin systemAdmin,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {
        try {
            adminUserManagementService.update(systemAdmin, loggedUser);
            redirectAttributes.addFlashAttribute("message", "Administrador atualizado com sucesso.");
            redirectAttributes.addFlashAttribute("error", false);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("error", true);
            log.error("Erro ao atualizar administrador: {}", e.getMessage());
        }
        return "redirect:/admin/admin-user-management";
    }

    @PostMapping("/activate")
    public String activateAdmin(@RequestParam("id") Long id,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {
        try {
            adminUserManagementService.activate(id, loggedUser);
            redirectAttributes.addFlashAttribute("message", "Administrador ativado com sucesso.");
            redirectAttributes.addFlashAttribute("error", false);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("error", true);
            log.error("Erro ao ativar administrador: {}", e.getMessage());
        }
        return "redirect:/admin/admin-user-management";
    }

    @PostMapping("/deactivate")
    public String deactivateAdmin(@RequestParam("id") Long id,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {
        try {
            adminUserManagementService.deactivate(id, loggedUser);
            redirectAttributes.addFlashAttribute("message", "Administrador desativado com sucesso.");
            redirectAttributes.addFlashAttribute("error", false);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("error", true);
            log.error("Erro ao desativar administrador: {}", e.getMessage());
        }
        return "redirect:/admin/admin-user-management";
    }

    private String normalizeSort(String sort) {
        return ALLOWED_SORTS.contains(sort) ? sort : "creationDate";
    }

    private Sort.Direction normalizeDirection(String direction) {
        try {
            return Sort.Direction.valueOf(direction);
        } catch (Exception e) {
            return Sort.Direction.DESC;
        }
    }
}
