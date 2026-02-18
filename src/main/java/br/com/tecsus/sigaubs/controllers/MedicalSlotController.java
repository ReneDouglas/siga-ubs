package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.dtos.AvailableMedicalSlotsFormDTO;
import br.com.tecsus.sigaubs.entities.MedicalProcedure;
import br.com.tecsus.sigaubs.entities.MedicalSlot;
import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.services.AppointmentService;
import br.com.tecsus.sigaubs.services.BasicHealthUnitService;
import br.com.tecsus.sigaubs.services.MedicalSlotService;
import br.com.tecsus.sigaubs.services.SpecialtyService;
import br.com.tecsus.sigaubs.services.exceptions.DistinctAvailableMedicalSlotException;
import br.com.tecsus.sigaubs.utils.DefaultValues;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@SessionScope
public class MedicalSlotController {

    private final BasicHealthUnitService basicHealthUnitService;
    private final SpecialtyService specialtyService;
    private final MedicalSlotService medicalSlotService;
    private final AppointmentService appointmentService;
    private AvailableMedicalSlotsFormDTO availableMedicalSlotsFormDTO;

    public MedicalSlotController(BasicHealthUnitService basicHealthUnitService, SpecialtyService specialtyService,
            MedicalSlotService medicalSlotService, AppointmentService appointmentService) {
        this.basicHealthUnitService = basicHealthUnitService;
        this.specialtyService = specialtyService;
        this.medicalSlotService = medicalSlotService;
        this.appointmentService = appointmentService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @GetMapping("/medicalSlot-management")
    public String getMedicalSlotPage(Model model) {

        this.availableMedicalSlotsFormDTO = null;

        model.addAttribute("basicHealthUnits", basicHealthUnitService.findAllUBS());
        model.addAttribute("specialties", specialtyService.findSpecialties());
        model.addAttribute("medicalSlotsPage",
                medicalSlotService.findMedicalSlotsPaginated(PageRequest.of(0, DefaultValues.PAGE_SIZE)));

        return "medicalSlotManagement/medicalSlot-management";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @PostMapping("/medicalSlot-management/slots/add")
    public String addAvailableMedicalSlotsRow(@ModelAttribute MedicalSlot availableMedicalSlot,
            Model model) {

        if (availableMedicalSlotsFormDTO == null) {
            this.availableMedicalSlotsFormDTO = new AvailableMedicalSlotsFormDTO();
        }

        BasicHealthUnit bhu = basicHealthUnitService
                .findSystemUserUBS(availableMedicalSlot.getBasicHealthUnit().getId());
        availableMedicalSlot.setBasicHealthUnit(bhu);

        availableMedicalSlot = basicHealthUnitService.getFetchedAssociations(availableMedicalSlot);

        this.availableMedicalSlotsFormDTO.addRow(availableMedicalSlot);
        model.addAttribute("availableMedicalSlotsForm", availableMedicalSlotsFormDTO);

        return "medicalSlotManagement/medicalSlotFragments/availableMedicalSlotsForm :: availableMedicalSlotsFormTable";

    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @PostMapping("/medicalSlot-management/slots/create")
    public String registerAvailableMedicalSlots(
            @ModelAttribute AvailableMedicalSlotsFormDTO availableMedicalSlotsFormDTO,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {

        try {
            medicalSlotService.registerAvailableMedicalSlotsBatch(availableMedicalSlotsFormDTO, loggedUser);
            redirectAttributes.addFlashAttribute("message", "Vagas registradas com sucesso.");
            redirectAttributes.addFlashAttribute("error", false);
            log.info("Vagas registradas com sucesso.");
        } catch (DistinctAvailableMedicalSlotException e) {
            log.error("Erro ao registrar vagas: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("message", "Erro ao registrar vagas: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", true);
        }

        this.availableMedicalSlotsFormDTO = null;
        return "redirect:/medicalSlot-management";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @GetMapping("/medicalSlot-management/slots/{index}/remove")
    public String removeRowtByIndex(@PathVariable int index,
            Model model) {
        this.availableMedicalSlotsFormDTO.removeRow(index);
        model.addAttribute("availableMedicalSlotsForm", this.availableMedicalSlotsFormDTO);
        return "medicalSlotManagement/medicalSlotFragments/availableMedicalSlotsForm :: availableMedicalSlotsFormTable";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @GetMapping("/medicalSlot-management/paginated")
    public String getMedicalSlotsPaginated(Model model,
            @RequestParam(value = "page", defaultValue = "0", required = false) int currentPage,
            @RequestParam(value = "pageSize", defaultValue = ""
                    + DefaultValues.PAGE_SIZE, required = false) int pageSize) {

        model.addAttribute("medicalSlotsPage",
                medicalSlotService.findMedicalSlotsPaginated(PageRequest.of(currentPage, pageSize)));
        return "medicalSlotManagement/medicalSlotFragments/medicalSlot-datatable :: medicalSlotsDatatable";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @GetMapping(value = "/medicalSlot-management/procedures", produces = MediaType.TEXT_HTML_VALUE)
    public String loadProcedure(@RequestParam("procedureType") String procedureType,
            @RequestParam("specialty") Long specialtyId,
            Model model) {

        List<MedicalProcedure> procedures;

        if (procedureType.equals(ProcedureType.CONSULTA.toString())) {
            procedures = appointmentService.findBySpecialtyIdAndProcedureType(specialtyId, ProcedureType.CONSULTA);
            model.addAttribute("isConsultation", true);
        } else if (procedureType.equals(ProcedureType.EXAME.toString())) {
            procedures = appointmentService.findBySpecialtyIdAndProcedureType(specialtyId, ProcedureType.EXAME);
        } else {
            procedures = appointmentService.findBySpecialtyIdAndProcedureType(specialtyId, ProcedureType.CIRURGIA);
        }

        model.addAttribute("procedures", procedures);

        return "medicalSlotManagement/medicalSlotFragments/medicalProcedures :: medicalProcedures";
    }
}
