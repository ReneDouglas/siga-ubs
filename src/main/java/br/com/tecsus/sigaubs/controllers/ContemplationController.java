package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.Specialty;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.services.BasicHealthUnitService;
import br.com.tecsus.sigaubs.services.ContemplationService;
import br.com.tecsus.sigaubs.services.SpecialtyService;
import br.com.tecsus.sigaubs.services.exceptions.CancelContemplationException;
import br.com.tecsus.sigaubs.services.exceptions.ConfirmContemplationException;
import br.com.tecsus.sigaubs.utils.DefaultValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.YearMonth;
import java.util.List;


@Controller
@SessionScope
public class ContemplationController {

    private static final Logger log = LoggerFactory.getLogger(ContemplationController.class);

    private final ContemplationService contemplationService;
    private final List<BasicHealthUnit> basicHealthUnits;
    private final List<Specialty> specialties;

    @Autowired
    public ContemplationController(BasicHealthUnitService basicHealthUnitService, SpecialtyService specialtyService, ContemplationService contemplationService) {
        this.contemplationService = contemplationService;
        this.basicHealthUnits = basicHealthUnitService.findAllUBS();
        this.specialties = specialtyService.findSpecialties();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @GetMapping("/contemplation-management")
    public String getContemplationPage(Model model) {

        var consultas = contemplationService
                .findContemplationsByUBSAndSpecialty(
                        ProcedureType.CONSULTA,
                        null,
                        null,
                        "",
                        "",
                        PageRequest.of(0, DefaultValues.PAGE_SIZE));
        var exames = contemplationService
                .findContemplationsByUBSAndSpecialty(
                        ProcedureType.EXAME,
                        null,
                        null,
                        "",
                        "",
                        PageRequest.of(0, DefaultValues.PAGE_SIZE));
        var cirurgias = contemplationService
                .findContemplationsByUBSAndSpecialty(
                        ProcedureType.CIRURGIA,
                        null,
                        null,
                        YearMonth.now().toString(),
                        "",
                        PageRequest.of(0, DefaultValues.PAGE_SIZE));

        model.addAttribute("basicHealthUnits", this.basicHealthUnits);
        model.addAttribute("specialties", this.specialties);
        model.addAttribute("consultasPage", consultas);
        model.addAttribute("examesPage", exames);
        model.addAttribute("cirurgiasPage", cirurgias);
        model.addAttribute("hide", "hidden");
        return "contemplationManagement/contemplation-management";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @GetMapping("/contemplation-management/search")
    public String loadSearchContemplations(@RequestParam(required = false) Long basicHealthUnit,
                                           @RequestParam(required = false) Long specialty,
                                           @RequestParam(required = false) String referenceMonth,
                                           @RequestParam(required = false) String status,
                                           Model model) {


        var consultas = contemplationService
                .findContemplationsByUBSAndSpecialty(
                        ProcedureType.CONSULTA,
                        basicHealthUnit,
                        specialty,
                        referenceMonth,
                        status,
                        PageRequest.of(0, DefaultValues.PAGE_SIZE));
        var exames = contemplationService
                .findContemplationsByUBSAndSpecialty(
                        ProcedureType.EXAME,
                        basicHealthUnit,
                        specialty,
                        referenceMonth,
                        status,
                        PageRequest.of(0, DefaultValues.PAGE_SIZE));
        var cirurgias = contemplationService
                .findContemplationsByUBSAndSpecialty(
                        ProcedureType.CIRURGIA,
                        basicHealthUnit,
                        specialty,
                        referenceMonth,
                        status,
                        PageRequest.of(0, DefaultValues.PAGE_SIZE));

        model.addAttribute("selectedUBS", basicHealthUnit);
        model.addAttribute("basicHealthUnits", this.basicHealthUnits);
        model.addAttribute("selectedSpecialty", specialty);
        model.addAttribute("selectedMonth", referenceMonth);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("specialties", this.specialties);
        model.addAttribute("consultasPage", consultas);
        model.addAttribute("examesPage", exames);
        model.addAttribute("cirurgiasPage", cirurgias);
        model.addAttribute("hide", "hidden");

        return "contemplationManagement/contemplation-management";
        /*return List.of(
                new ModelAndView("contemplationManagement/contemplationFragments/contemplation-tabs :: consultas-datatable",
                        Map.of("consultasPage", consultas)),
                new ModelAndView("contemplationManagement/contemplationFragments/contemplation-tabs :: exames-datatable",
                        Map.of("examesPage", exames)),
                new ModelAndView("contemplationManagement/contemplationFragments/contemplation-tabs :: cirurgias-datatable",
                        Map.of("cirurgiasPage", cirurgias))
        );*/
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @GetMapping("/contemplation-management/paginated")
    public String getMedicalSlotsPaginated(Model model,
                                           @RequestParam(value = "page", defaultValue = "0", required = false) int currentPage,
                                           @RequestParam(value = "consultasPageSize", defaultValue = "" + DefaultValues.PAGE_SIZE, required = false) int consultasPageSize,
                                           @RequestParam(value = "examesPageSize", defaultValue = "" + DefaultValues.PAGE_SIZE, required = false) int examesPageSize,
                                           @RequestParam(value = "cirurgiasPageSize", defaultValue = "" + DefaultValues.PAGE_SIZE, required = false) int cirurgiasPageSize,
                                           @RequestParam(value = "ubs", required = false) Long ubs,
                                           @RequestParam(value = "specialty", required = false) Long specialty,
                                           @RequestParam(value = "month", required = false) String referenceMonth,
                                           @RequestParam(value = "type") String procedureType,
                                           @RequestParam(value = "status", required = false) String status) {

        model.addAttribute("selectedUBS", ubs);
        model.addAttribute("selectedSpecialty", specialty);
        model.addAttribute("selectedMonth", referenceMonth);
        model.addAttribute("selectedStatus", status);

        if (procedureType.equals(ProcedureType.CONSULTA.toString())) {
            model.addAttribute("consultasPage", contemplationService
                    .findContemplationsByUBSAndSpecialty(
                            ProcedureType.CONSULTA,
                            ubs,
                            specialty,
                            referenceMonth,
                            status,
                            PageRequest.of(currentPage, consultasPageSize)));
            return "contemplationManagement/contemplationFragments/contemplation-tabs :: consultas-datatable";
        } else if (procedureType.equals(ProcedureType.EXAME.toString())) {
            model.addAttribute("examesPage", contemplationService
                    .findContemplationsByUBSAndSpecialty(
                            ProcedureType.EXAME,
                            ubs,
                            specialty,
                            referenceMonth,
                            status,
                            PageRequest.of(currentPage, examesPageSize)));
            return "contemplationManagement/contemplationFragments/contemplation-tabs :: exames-datatable";
        } else if (procedureType.equals(ProcedureType.CIRURGIA.toString())) {
            model.addAttribute("cirurgiasPage", contemplationService
                    .findContemplationsByUBSAndSpecialty(
                            ProcedureType.CIRURGIA,
                            ubs,
                            specialty,
                            referenceMonth,
                            status,
                            PageRequest.of(currentPage, cirurgiasPageSize)));
            return "contemplationManagement/contemplationFragments/contemplation-tabs :: cirurgias-datatable";

        }
        return "contemplationManagement/contemplation-management";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @GetMapping("/contemplation-management/{id}/load")
    public String loadContemplated(@PathVariable Long id,
                                   @RequestParam(required = false) Long ubs,
                                   @RequestParam(required = false) Long specialty,
                                   @RequestParam(required = false) String month,
                                   Model model) {

        model.addAttribute("selectedUBS", ubs);
        model.addAttribute("selectedSpecialty", specialty);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("contemplated", contemplationService.loadContemplatedById(id));
        model.addAttribute("isContemplated", true);

        //return "contemplationManagement/contemplationFragments/contemplation-info :: contemplationInfo";
        return "queueManagement/queueFragments/patientAppointment-info :: patientAppointmentInfo";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @PostMapping(value = "/contemplation-management/cancel")
    public String cancelContemplation(@RequestParam("reason") String reason,
                                      @RequestParam(required = false) Long ubs,
                                      @RequestParam(required = false) Long specialty,
                                      @RequestParam(required = false) String month,
                                      @RequestParam Long contemplationId,
                                      @AuthenticationPrincipal SystemUserDetails loggedUser,
                                      RedirectAttributes redirectAttributes) {


        try {
            log.info("Cancelando contemplação[id={}] pelo usuário[nome={}].", contemplationId, loggedUser.getName());
            contemplationService.cancelContemplationByAdmin(contemplationId, reason, loggedUser);
            redirectAttributes.addFlashAttribute("error", false);
            redirectAttributes.addFlashAttribute("message", "Contemplação cancelada com sucesso.");
            log.info("Contemplação[id={}] cancelada com sucesso pelo usuário[nome={}].", contemplationId, loggedUser.getName());
        } catch (CancelContemplationException e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("message", "Erro ao cancelar contemplação.");
            log.error("Erro ao cancelar contemplação: {}", e.getMessage());
        }

        UriComponentsBuilder redirectURL = UriComponentsBuilder.fromPath("/contemplation-management/search");

        if (ubs != null) redirectURL.queryParam("basicHealthUnit", ubs);
        if (specialty != null) redirectURL.queryParam("specialty", specialty);
        if (month != null && !month.isEmpty()) redirectURL.queryParam("referenceMonth", month);

        return "redirect:" + redirectURL.toUriString();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @PostMapping(value = "/contemplation-management/confirm")
    public String confirmContemplation(@RequestParam(required = false) Long ubs,
                                       @RequestParam(required = false) Long specialty,
                                       @RequestParam(required = false) String month,
                                       @RequestParam Long contemplationId,
                                       @AuthenticationPrincipal SystemUserDetails loggedUser,
                                       RedirectAttributes redirectAttributes) {

        try {
            contemplationService.confirmContemplationByAdmin(contemplationId, loggedUser);
            redirectAttributes.addFlashAttribute("error", false);
            redirectAttributes.addFlashAttribute("message", "Contemplação confirmada com sucesso.");
            log.info("Contemplação[id={}] confirmada com sucesso pelo usuário[nome={}].", contemplationId, loggedUser.getName());
        } catch (ConfirmContemplationException e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("message", "Erro ao confirmar contemplação.");
            log.error("Erro ao confirmar contemplação: {}", e.getMessage());
        }

        UriComponentsBuilder redirectURL = UriComponentsBuilder.fromPath("/contemplation-management/search");

        if (ubs != null) redirectURL.queryParam("basicHealthUnit", ubs);
        if (specialty != null) redirectURL.queryParam("specialty", specialty);
        if (month != null && !month.isEmpty()) redirectURL.queryParam("referenceMonth", month);

        return "redirect:" + redirectURL.toUriString();
    }


}
