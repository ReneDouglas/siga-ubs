package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.dtos.AdminUserSearchDTO;
import br.com.tecsus.sigaubs.dtos.AdminAccountCommandDTO;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.entities.SystemAdmin;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.services.AdminUserManagementService;
import br.com.tecsus.sigaubs.utils.PaginationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.Locale;
import java.util.Set;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

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
            @RequestParam(value = "pageSize", defaultValue = ""
                    + PaginationPolicy.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sort", defaultValue = "creationDate", required = false) String sort,
            @RequestParam(value = "direction", defaultValue = "DESC", required = false) String direction,
            @RequestParam(value = "pagination", defaultValue = "false", required = false) boolean pagination,
            @AuthenticationPrincipal SystemUserDetails loggedUser) {

        String sortProperty = normalizeSort(sort);
        Sort.Direction sortDirection = normalizeDirection(direction);
        model.addAttribute("adminsPage", adminUserManagementService.findAdmins(
                searchAdmin,
                PaginationPolicy.pageRequest(
                        currentPage, pageSize, sortDirection, sortProperty)));
        model.addAttribute("selectedSort", sortProperty);
        model.addAttribute("selectedDirection", sortDirection.name());
        model.addAttribute("currentUsername", loggedUser.getLoginUsername());

        if (pagination) {
            return "adminUserManagement/adminUserFragments/admin_user_datatable";
        }

        model.addAttribute("systemAdmin", loadAdminForForm(adminId, model));
        return "adminUserManagement/admin_user_management";
    }

    @PostMapping("/create")
    public String createAdmin(@Valid @ModelAttribute AdminAccountCommandDTO command,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("message", "Verifique os campos e a política de senha.");
            redirectAttributes.addFlashAttribute("error", true);
            return "redirect:/admin/admin-user-management";
        }
        var resultado = adminUserManagementService.create(command, loggedUser);
        addFlashResult(redirectAttributes, resultado, "Administrador cadastrado com sucesso.");
        logResult("cadastrar administrador", resultado);
        return "redirect:/admin/admin-user-management";
    }

    @PostMapping("/update")
    public String updateAdmin(@Valid @ModelAttribute AdminAccountCommandDTO command,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("message", "Verifique os campos e a política de senha.");
            redirectAttributes.addFlashAttribute("error", true);
            return "redirect:/admin/admin-user-management";
        }
        var resultado = adminUserManagementService.update(command, loggedUser);
        addFlashResult(redirectAttributes, resultado, "Administrador atualizado com sucesso.");
        logResult("atualizar administrador", resultado);
        return "redirect:/admin/admin-user-management";
    }

    @PostMapping("/activate")
    public String activateAdmin(@RequestParam("id") Long id,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {
        var resultado = adminUserManagementService.activate(id, loggedUser);
        addFlashResult(redirectAttributes, resultado, "Administrador ativado com sucesso.");
        logResult("ativar administrador", resultado);
        return "redirect:/admin/admin-user-management";
    }

    @PostMapping("/deactivate")
    public String deactivateAdmin(@RequestParam("id") Long id,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {
        var resultado = adminUserManagementService.deactivate(id, loggedUser);
        addFlashResult(redirectAttributes, resultado, "Administrador desativado com sucesso.");
        logResult("desativar administrador", resultado);
        return "redirect:/admin/admin-user-management";
    }

    private String normalizeSort(String sort) {
        return ALLOWED_SORTS.contains(sort) ? sort : "creationDate";
    }

    private Sort.Direction normalizeDirection(String direction) {
        if (direction == null) {
            return Sort.Direction.DESC;
        }
        return switch (direction.trim().toUpperCase(Locale.ROOT)) {
            case "ASC" -> Sort.Direction.ASC;
            case "DESC" -> Sort.Direction.DESC;
            default -> Sort.Direction.DESC;
        };
    }

    private void addFlashResult(RedirectAttributes redirectAttributes,
            ResultadoOperacao<?> resultado,
            String successMessage) {
        redirectAttributes.addFlashAttribute("message", resultado.sucesso() ? successMessage : resultado.mensagem());
        redirectAttributes.addFlashAttribute("error", resultado.falhou());
    }

    private void logResult(String action, ResultadoOperacao<?> resultado) {
        if (resultado.falhou()) {
            log.error("Erro ao {}: {}", action, resultado.mensagem());
        }
    }

    private SystemAdmin loadAdminForForm(Long adminId, Model model) {
        if (adminId == null) {
            return new SystemAdmin();
        }

        var resultado = adminUserManagementService.findById(adminId);
        if (resultado.sucesso()) {
            return resultado.valor();
        }

        model.addAttribute("message", resultado.mensagem());
        model.addAttribute("error", true);
        log.error("Erro ao carregar administrador [id={}]: {}", adminId, resultado.mensagem());
        return new SystemAdmin();
    }
}
