package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.dtos.SmsUserSearchDTO;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.dtos.AdminAccountCommandDTO;
import br.com.tecsus.sigaubs.entities.SystemUser;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.services.AdminSmsUserService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;
import java.util.Set;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

@Controller
@RequestMapping("/admin/tenant-management/{tenantId}/sms-users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSmsUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminSmsUserController.class);
    private static final Set<String> ALLOWED_SORTS = Set.of("username", "name", "email", "active", "creationDate", "updateDate");

    private final AdminSmsUserService adminSmsUserService;

    public AdminSmsUserController(AdminSmsUserService adminSmsUserService) {
        this.adminSmsUserService = adminSmsUserService;
    }

    @GetMapping
    public String getSmsUsersPage(@PathVariable Long tenantId,
            Model model,
            @ModelAttribute("searchUser") SmsUserSearchDTO searchUser,
            @RequestParam(value = "editUserId", required = false) Long editUserId,
            @RequestParam(value = "page", defaultValue = "0", required = false) int currentPage,
            @RequestParam(value = "pageSize", defaultValue = ""
                    + PaginationPolicy.DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sort", defaultValue = "creationDate", required = false) String sort,
            @RequestParam(value = "direction", defaultValue = "DESC", required = false) String direction,
            @RequestParam(value = "pagination", defaultValue = "false", required = false) boolean pagination) {

        String sortProperty = normalizeSort(sort);
        Sort.Direction sortDirection = normalizeDirection(direction);
        model.addAttribute("tenant", adminSmsUserService.findTenant(tenantId));
        model.addAttribute("smsUsersPage", adminSmsUserService.findSmsUsers(
                tenantId,
                searchUser,
                PaginationPolicy.pageRequest(
                        currentPage, pageSize, sortDirection, sortProperty)));
        model.addAttribute("selectedSort", sortProperty);
        model.addAttribute("selectedDirection", sortDirection.name());

        if (pagination) {
            return "tenantManagement/smsUserFragments/sms_user_datatable";
        }

        model.addAttribute("systemUser", editUserId != null
                ? adminSmsUserService.findSmsUser(tenantId, editUserId)
                : new SystemUser());
        return "tenantManagement/sms_user_management";
    }

    @PostMapping("/create")
    public String createSmsUser(@PathVariable Long tenantId,
            @Valid @ModelAttribute AdminAccountCommandDTO command,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("message", "Verifique os campos e a política de senha.");
            redirectAttributes.addFlashAttribute("error", true);
            return "redirect:/admin/tenant-management/" + tenantId + "/sms-users";
        }
        var resultado = adminSmsUserService.createSmsUser(tenantId, command, loggedUser);
        addFlashResult(redirectAttributes, resultado, "Usuário SMS cadastrado com sucesso.");
        logResult("cadastrar usuário SMS", resultado);
        return "redirect:/admin/tenant-management/" + tenantId + "/sms-users";
    }

    @PostMapping("/update")
    public String updateSmsUser(@PathVariable Long tenantId,
            @Valid @ModelAttribute AdminAccountCommandDTO command,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("message", "Verifique os campos e a política de senha.");
            redirectAttributes.addFlashAttribute("error", true);
            return "redirect:/admin/tenant-management/" + tenantId + "/sms-users";
        }
        var resultado = adminSmsUserService.updateSmsUser(tenantId, command, loggedUser);
        addFlashResult(redirectAttributes, resultado, "Usuário SMS atualizado com sucesso.");
        logResult("atualizar usuário SMS", resultado);
        return "redirect:/admin/tenant-management/" + tenantId + "/sms-users";
    }

    @PostMapping("/activate")
    public String activateSmsUser(@PathVariable Long tenantId,
            @RequestParam("id") Long id,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {
        var resultado = adminSmsUserService.activateSmsUser(tenantId, id, loggedUser);
        addFlashResult(redirectAttributes, resultado, "Usuário SMS ativado com sucesso.");
        logResult("ativar usuário SMS", resultado);
        return "redirect:/admin/tenant-management/" + tenantId + "/sms-users";
    }

    @PostMapping("/deactivate")
    public String deactivateSmsUser(@PathVariable Long tenantId,
            @RequestParam("id") Long id,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {
        var resultado = adminSmsUserService.deactivateSmsUser(tenantId, id, loggedUser);
        addFlashResult(redirectAttributes, resultado, "Usuário SMS desativado com sucesso.");
        logResult("desativar usuário SMS", resultado);
        return "redirect:/admin/tenant-management/" + tenantId + "/sms-users";
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
}
