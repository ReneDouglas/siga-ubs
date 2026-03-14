package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.dtos.ProcedureDTO;
import br.com.tecsus.sigaubs.dtos.SpecialtyDTO;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.services.SpecialtyService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@SessionScope
public class SpecialtyController {

    private static final Logger log = LoggerFactory.getLogger(SpecialtyController.class);

    private final SpecialtyService specialtyService;
    private final ObjectMapper objectMapper;

    public SpecialtyController(SpecialtyService specialtyService) {
        this.specialtyService = specialtyService;
        objectMapper = new ObjectMapper();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @GetMapping("/specialty-management")
    public String getSpecialtiesPage(Model model) {

        model.addAttribute("specialtyDTO", new SpecialtyDTO());
        model.addAttribute("specialties", specialtyService.findSpecialties());
        model.addAttribute("procedures", List.of());

        return "specialtyManagement/specialty-management";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @GetMapping("/specialty-management/specialty")
    public String getSpecialtyToEdit(Model model, @RequestParam("id") Long id) {

        SpecialtyDTO specialtyDTO = specialtyService.findFetchedSpecialty(id);

        model.addAttribute("specialtyDTO", specialtyDTO);
        model.addAttribute("specialties", specialtyService.findSpecialties());
        model.addAttribute("procedures", specialtyDTO.getProcedures());

        return "specialtyManagement/specialty-management";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @PostMapping("/specialty-management/create")
    public String registerSpecialty(@ModelAttribute SpecialtyDTO specialtyDTO,
                                    @RequestParam("proceduresJson") String proceduresJson,
                                    @AuthenticationPrincipal SystemUserDetails loggedUser,
                                    RedirectAttributes redirectAttributes) {

        try {
            var procedures = objectMapper.readValue(proceduresJson, new TypeReference<List<ProcedureDTO>>() {});
            specialtyDTO.setProcedures(procedures);
            specialtyService.registerSpecialty(specialtyDTO, loggedUser);
            redirectAttributes.addFlashAttribute("message", "Especialidade cadastrada com sucesso.");
            redirectAttributes.addFlashAttribute("error", false);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Erro ao cadastrar especialidade.");
            redirectAttributes.addFlashAttribute("error", true);
            log.error("Erro ao cadastrar especialidade: {}", e.getMessage());
        }
        return "redirect:/specialty-management";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @PostMapping("/specialty-management/update")
    public String updateSpecialty(@ModelAttribute SpecialtyDTO specialtyDTO,
                                  @RequestParam("proceduresJson") String proceduresJson,
                                  @AuthenticationPrincipal SystemUserDetails loggedUser,
                                  RedirectAttributes redirectAttributes) {

        try {
            var procedures = objectMapper.readValue(proceduresJson, new TypeReference<List<ProcedureDTO>>() {});
            specialtyDTO.setProcedures(procedures);
            specialtyService.updateSpecialty(specialtyDTO, loggedUser);
            redirectAttributes.addFlashAttribute("message", "Especialidade atualizada com sucesso.");
            redirectAttributes.addFlashAttribute("error", false);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Erro ao atualizar especialidade.");
            redirectAttributes.addFlashAttribute("error", true);
            log.error("Erro ao atualizar especialidade: {}", e.getMessage());
        }
        return "redirect:/specialty-management";
    }

}
