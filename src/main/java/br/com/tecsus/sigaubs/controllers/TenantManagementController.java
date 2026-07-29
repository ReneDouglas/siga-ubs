package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.dtos.TenantSearchDTO;
import br.com.tecsus.sigaubs.dtos.TenantCommandDTO;
import br.com.tecsus.sigaubs.entities.Tenant;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.services.SystemMaintenanceService;
import br.com.tecsus.sigaubs.services.TenantManagementService;
import br.com.tecsus.sigaubs.services.TenantSessionService;
import br.com.tecsus.sigaubs.utils.DefaultValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

@Controller
@RequestMapping("/admin/tenant-management")
@PreAuthorize("hasRole('ADMIN')")
public class TenantManagementController {

    private static final Logger log = LoggerFactory.getLogger(TenantManagementController.class);
    private static final Set<String> ALLOWED_SORTS = Set.of("slug", "name", "domain", "status", "creationDate", "updateDate");

    private final TenantManagementService tenantManagementService;
    private final SystemMaintenanceService systemMaintenanceService;
    private final TenantSessionService tenantSessionService;

    public TenantManagementController(TenantManagementService tenantManagementService,
            SystemMaintenanceService systemMaintenanceService,
            TenantSessionService tenantSessionService) {
        this.tenantManagementService = tenantManagementService;
        this.systemMaintenanceService = systemMaintenanceService;
        this.tenantSessionService = tenantSessionService;
    }

    @GetMapping
    public String getTenantManagementPage(Model model,
            @ModelAttribute("searchTenant") TenantSearchDTO searchTenant,
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            @RequestParam(value = "page", defaultValue = "0", required = false) int currentPage,
            @RequestParam(value = "pageSize", defaultValue = "" + DefaultValues.PAGE_SIZE, required = false) int pageSize,
            @RequestParam(value = "sort", defaultValue = "creationDate", required = false) String sort,
            @RequestParam(value = "direction", defaultValue = "DESC", required = false) String direction,
            @RequestParam(value = "pagination", defaultValue = "false", required = false) boolean pagination) {

        String sortProperty = normalizeSort(sort);
        Sort.Direction sortDirection = normalizeDirection(direction);
        model.addAttribute("tenantsPage", tenantManagementService.findTenantsPaginated(
                searchTenant,
                PageRequest.of(Math.max(0, currentPage), Math.clamp(pageSize, 1, 100),
                        sortDirection, sortProperty)));
        model.addAttribute("selectedSort", sortProperty);
        model.addAttribute("selectedDirection", sortDirection.name());
        model.addAttribute("tenantStatuses", tenantManagementService.getStatuses());

        if (pagination) {
            return "tenantManagement/tenantFragments/tenant_datatable";
        }

        model.addAttribute("tenant", loadTenantForForm(tenantId, model));
        model.addAttribute("systemMaintenance", systemMaintenanceService.getCurrent());
        return "tenantManagement/tenant_management";
    }

    @PostMapping("/create")
    public String createTenant(@Valid @ModelAttribute TenantCommandDTO command,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("message", "Verifique os campos do tenant.");
            redirectAttributes.addFlashAttribute("error", true);
            return "redirect:/admin/tenant-management";
        }
        var resultado = tenantManagementService.create(command, loggedUser);
        addFlashResult(redirectAttributes, resultado, "Tenant cadastrado com sucesso.");
        logResult("cadastrar tenant", resultado);
        return "redirect:/admin/tenant-management";
    }

    @PostMapping("/update")
    public String updateTenant(@Valid @ModelAttribute TenantCommandDTO command,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("message", "Verifique os campos do tenant.");
            redirectAttributes.addFlashAttribute("error", true);
            return "redirect:/admin/tenant-management";
        }
        var resultado = tenantManagementService.update(command, loggedUser);
        addFlashResult(redirectAttributes, resultado, "Tenant atualizado com sucesso.");
        logResult("atualizar tenant", resultado);
        return "redirect:/admin/tenant-management";
    }

    @PostMapping("/disable")
    public String disableTenant(@RequestParam("id") Long id,
            @RequestParam(value = "disabledReason", required = false) String disabledReason,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {
        var resultado = tenantManagementService.disable(id, disabledReason, loggedUser);
        addFlashResult(redirectAttributes, resultado, "Tenant desabilitado com sucesso.");
        logResult("desabilitar tenant", resultado);
        return "redirect:/admin/tenant-management";
    }

    @PostMapping("/reactivate")
    public String reactivateTenant(@RequestParam("id") Long id,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {
        var resultado = tenantManagementService.reactivate(id, loggedUser);
        addFlashResult(redirectAttributes, resultado, "Tenant reativado com sucesso.");
        logResult("reativar tenant", resultado);
        return "redirect:/admin/tenant-management";
    }

    @PostMapping("/maintenance/start")
    public String startTenantMaintenance(@RequestParam("id") Long id,
            @RequestParam(value = "maintenanceMessage", required = false) String maintenanceMessage,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {
        var resultado = tenantManagementService.startMaintenance(id, maintenanceMessage, loggedUser);
        addFlashResult(redirectAttributes, resultado, "Tenant colocado em manutenção.");
        logResult("colocar tenant em manutenção", resultado);
        return "redirect:/admin/tenant-management";
    }

    @PostMapping("/maintenance/end")
    public String endTenantMaintenance(@RequestParam("id") Long id,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {
        var resultado = tenantManagementService.endMaintenance(id, loggedUser);
        addFlashResult(redirectAttributes, resultado, "Manutenção do tenant encerrada.");
        logResult("encerrar manutenção do tenant", resultado);
        return "redirect:/admin/tenant-management";
    }

    @PostMapping("/slug")
    public String updateTenantSlug(@RequestParam("id") Long id,
            @RequestParam("newSlug") String newSlug,
            @RequestParam("confirmation") String confirmation,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {
        var resultado = tenantManagementService.updateSlug(id, newSlug, confirmation, loggedUser);
        addFlashResult(redirectAttributes, resultado, "Slug alterado com sucesso. Sessões do tenant foram encerradas.");
        logResult("alterar slug do tenant", resultado);
        return "redirect:/admin/tenant-management";
    }

    @PostMapping("/global-maintenance")
    public String updateGlobalMaintenance(
            @RequestParam(value = "enabled", defaultValue = "false") Boolean enabled,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime endDate,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {
        try {
            boolean wasEnabled = systemMaintenanceService.isEnabled();
            systemMaintenanceService.update(enabled, message, endDate, loggedUser);
            if (!wasEnabled && Boolean.TRUE.equals(enabled)) {
                tenantSessionService.expireTenantScopedSessions();
            }
            redirectAttributes.addFlashAttribute("message", "Manutenção global atualizada com sucesso.");
            redirectAttributes.addFlashAttribute("error", false);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("error", true);
            log.error("Erro ao atualizar manutenção global: {}", e.getMessage());
        }
        return "redirect:/admin/tenant-management";
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

    private Tenant loadTenantForForm(Long tenantId, Model model) {
        if (tenantId == null) {
            return new Tenant();
        }

        var resultado = tenantManagementService.findById(tenantId);
        if (resultado.sucesso()) {
            return resultado.valor();
        }

        model.addAttribute("message", resultado.mensagem());
        model.addAttribute("error", true);
        log.error("Erro ao carregar tenant [id={}]: {}", tenantId, resultado.mensagem());
        return new Tenant();
    }
}
