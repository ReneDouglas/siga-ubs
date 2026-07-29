package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.dtos.MedicalSlotBatchCommandDTO;
import br.com.tecsus.sigaubs.dtos.MedicalSlotCommandDTO;
import br.com.tecsus.sigaubs.entities.MedicalProcedure;
import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.services.AppointmentService;
import br.com.tecsus.sigaubs.services.BasicHealthUnitService;
import br.com.tecsus.sigaubs.services.MedicalSlotService;
import br.com.tecsus.sigaubs.services.SpecialtyService;
import br.com.tecsus.sigaubs.utils.DefaultValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

@Controller
public class MedicalSlotController {

    private static final Logger log = LoggerFactory.getLogger(MedicalSlotController.class);

    private final BasicHealthUnitService basicHealthUnitService;
    private final SpecialtyService specialtyService;
    private final MedicalSlotService medicalSlotService;
    private final AppointmentService appointmentService;

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

        model.addAttribute("basicHealthUnits", basicHealthUnitService.findAllUBS());
        model.addAttribute("specialties", specialtyService.findSpecialties());
        model.addAttribute("medicalSlotsPage",
                medicalSlotService.findMedicalSlotsPaginated(PageRequest.of(0, DefaultValues.PAGE_SIZE)));

        return "medicalSlotManagement/medicalSlot_management";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @PostMapping("/medicalSlot-management/slots/add")
    public String addAvailableMedicalSlotsRow(
            @Valid @ModelAttribute MedicalSlotCommandDTO availableMedicalSlot,
            BindingResult rowBindingResult,
            @Valid @ModelAttribute MedicalSlotBatchCommandDTO availableMedicalSlotsFormDTO,
            BindingResult batchBindingResult,
            Model model) {

        if (rowBindingResult.hasErrors() || batchBindingResult.hasErrors()) {
            model.addAttribute("availableMedicalSlotsForm", hydrateForm(availableMedicalSlotsFormDTO));
            return "medicalSlotManagement/medicalSlotFragments/available_slots_form_table";
        }
        availableMedicalSlotsFormDTO = hydrateForm(availableMedicalSlotsFormDTO);
        availableMedicalSlotsFormDTO.addRow(hydrateSlot(availableMedicalSlot));
        model.addAttribute("availableMedicalSlotsForm", availableMedicalSlotsFormDTO);

        return "medicalSlotManagement/medicalSlotFragments/available_slots_form_table";

    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @PostMapping("/medicalSlot-management/slots/create")
    public String registerAvailableMedicalSlots(
            @Valid @ModelAttribute MedicalSlotBatchCommandDTO availableMedicalSlotsFormDTO,
            BindingResult bindingResult,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {

        var resultado = bindingResult.hasErrors()
                ? br.com.tecsus.sigaubs.dtos.ResultadoOperacao.<Void>falha("Lote de vagas inválido.")
                : medicalSlotService.registerAvailableMedicalSlotsBatch(availableMedicalSlotsFormDTO, loggedUser);
        if (resultado.sucesso()) {
            redirectAttributes.addFlashAttribute("message", "Vagas registradas com sucesso.");
            redirectAttributes.addFlashAttribute("error", false);
            log.info("Vagas registradas com sucesso.");
        } else {
            log.error("Erro ao registrar vagas: {}", resultado.mensagem());
            redirectAttributes.addFlashAttribute("message", "Erro ao registrar vagas: " + resultado.mensagem());
            redirectAttributes.addFlashAttribute("error", true);
        }

        return "redirect:/medicalSlot-management";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @PostMapping("/medicalSlot-management/slots/{index}/remove")
    public String removeRowtByIndex(@PathVariable int index,
            @Valid @ModelAttribute MedicalSlotBatchCommandDTO availableMedicalSlotsFormDTO,
            BindingResult bindingResult,
            Model model) {
        availableMedicalSlotsFormDTO = hydrateForm(availableMedicalSlotsFormDTO);
        if (index >= 0 && index < availableMedicalSlotsFormDTO.getAvailableMedicalSlots().size()) {
            availableMedicalSlotsFormDTO.removeRow(index);
        }
        model.addAttribute("availableMedicalSlotsForm", availableMedicalSlotsFormDTO);
        return "medicalSlotManagement/medicalSlotFragments/available_slots_form_table";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @GetMapping("/medicalSlot-management/paginated")
    public String getMedicalSlotsPaginated(Model model,
            @RequestParam(value = "page", defaultValue = "0", required = false) int currentPage,
            @RequestParam(value = "pageSize", defaultValue = ""
                    + DefaultValues.PAGE_SIZE, required = false) int pageSize) {

        model.addAttribute("medicalSlotsPage",
                medicalSlotService.findMedicalSlotsPaginated(
                        PageRequest.of(Math.max(0, currentPage), Math.clamp(pageSize, 1, 100))));
        return "medicalSlotManagement/medicalSlotFragments/medicalSlot_datatable";
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

        return "medicalSlotManagement/medicalSlotFragments/medicalProcedures";
    }

    private MedicalSlotBatchCommandDTO hydrateForm(MedicalSlotBatchCommandDTO form) {
        MedicalSlotBatchCommandDTO hydratedForm = form != null ? form : new MedicalSlotBatchCommandDTO();
        List<MedicalSlotCommandDTO> hydratedSlots = new ArrayList<>();
        if (hydratedForm.getAvailableMedicalSlots() != null) {
            for (MedicalSlotCommandDTO slot : hydratedForm.getAvailableMedicalSlots()) {
                hydratedSlots.add(hydrateSlot(slot));
            }
        }
        hydratedForm.setAvailableMedicalSlots(hydratedSlots);
        return hydratedForm;
    }

    private MedicalSlotCommandDTO hydrateSlot(MedicalSlotCommandDTO slot) {
        if (slot == null) {
            return new MedicalSlotCommandDTO();
        }
        if (slot.getBasicHealthUnit() != null && slot.getBasicHealthUnit().getId() != null) {
            BasicHealthUnit bhu = basicHealthUnitService.findSystemUserUBS(slot.getBasicHealthUnit().getId());
            slot.setBasicHealthUnitName(bhu.getName());
        }
        if (slot.getMedicalProcedure() != null && slot.getMedicalProcedure().getId() != null) {
            MedicalProcedure procedure = basicHealthUnitService.fetchMedicalProcedure(
                    slot.getMedicalProcedure().getId());
            if (procedure != null) {
                slot.setProcedureDescription(procedure.getDescription());
                slot.setProcedureTypeDescription(procedure.getProcedureType().getDescription());
                slot.setSpecialtyTitle(procedure.getSpecialty().getTitle());
            }
        }
        return slot;
    }
}
