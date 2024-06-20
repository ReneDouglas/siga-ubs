package br.com.tecsus.sccubs.controllers;

import br.com.tecsus.sccubs.dtos.ProcedureDTO;
import br.com.tecsus.sccubs.dtos.SpecialtyDTO;
import br.com.tecsus.sccubs.security.SystemUserDetails;
import br.com.tecsus.sccubs.services.SpecialtyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
public class SpecialtyController {

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
    public String getSpecialtyToEdit(Model model, @RequestParam("id") long id) {

        SpecialtyDTO specialtyDTO = specialtyService.findFetchedSpecialty(id);

        model.addAttribute("specialtyDTO", specialtyDTO);
        model.addAttribute("specialties", specialtyService.findSpecialties());
        model.addAttribute("procedures", specialtyDTO.getProcedures());

        return "specialtyManagement/specialty-management";
    }

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
