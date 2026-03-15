package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.dtos.UBSsystemUserDTO;
import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.services.BasicHealthUnitService;
import br.com.tecsus.sigaubs.services.SpecialtyService;
import br.com.tecsus.sigaubs.services.SystemUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
public class BasicHealthUnitController {

    private static final Logger log = LoggerFactory.getLogger(BasicHealthUnitController.class);

    private final BasicHealthUnitService basicHealthUnitService;
    private final SystemUserService systemUserService;
    private final SpecialtyService specialtyService;

    @Autowired
    public BasicHealthUnitController(BasicHealthUnitService basicHealthUnitService,
                                     SystemUserService systemUserService,
                                     SpecialtyService specialtyService) {
        this.basicHealthUnitService = basicHealthUnitService;
        this.systemUserService = systemUserService;
        this.specialtyService = specialtyService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @GetMapping("/basicHealthUnit-management")
    public String getBasicHealthUnitPage(Model model, @AuthenticationPrincipal SystemUserDetails loggedUser) {

        BasicHealthUnit ubs = new BasicHealthUnit();

        model.addAttribute("basicHealthUnit", ubs);
        model.addAttribute("basicHealthUnits", basicHealthUnitService
                .findAllUBS());
        model.addAttribute("ubsUsers", List.of());
        model.addAttribute("specialties", specialtyService.findSpecialties());

        return "basicHealthUnitManagement/basicHealthUnit_management";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @PostMapping("/basicHealthUnit-management/create")
    public String registerBasicHealthUnit(@ModelAttribute BasicHealthUnit basicHealthUnit,
                                          @AuthenticationPrincipal SystemUserDetails loggedUser,
                                          RedirectAttributes redirectAttributes) {

        try {
            basicHealthUnitService.registerBasicHealthUnit(basicHealthUnit, loggedUser);
            redirectAttributes.addFlashAttribute("message", "UBS cadastrada com sucesso.");
            redirectAttributes.addFlashAttribute("error", false);
            log.info("UBS cadastrada com sucesso.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Erro ao cadastrar UBS.");
            redirectAttributes.addFlashAttribute("error", true);
            log.error("Erro ao cadastrar UBS: {}", e.getMessage());
        }

        return "redirect:/basicHealthUnit-management";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @PostMapping("/basicHealthUnit-management")
    public String getBasicHealthUnitPageToUpdate(@RequestParam(value = "basicHealthUnit", required = false) Long basicHealthUnit,
                                                 Model model) {

        if (basicHealthUnit == null) {
            return "redirect:/basicHealthUnit-management";
        }

        model.addAttribute("basicHealthUnit", basicHealthUnitService.findSystemUserUBS(basicHealthUnit));
        model.addAttribute("basicHealthUnits", basicHealthUnitService
                .findAllUBS());
        model.addAttribute("ubsUsers", List.of());

        return "basicHealthUnitManagement/basicHealthUnit_management";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @PostMapping("/basicHealthUnit-management/update")
    public String updateSystemUser(@ModelAttribute BasicHealthUnit basicHealthUnit,
                                   RedirectAttributes redirectAttributes,
                                   @AuthenticationPrincipal SystemUserDetails loggedUser) {

        try {
            basicHealthUnitService.updateBasicHealthUnit(basicHealthUnit, loggedUser);
            redirectAttributes.addFlashAttribute("message", "UBS atualizada com sucesso.");
            redirectAttributes.addFlashAttribute("error", false);
            log.info("UBS atualizada com sucesso.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Erro ao atualizar UBS.");
            redirectAttributes.addFlashAttribute("error", true);
            log.error("Erro ao atualizar UBS: {}", e.getMessage());
        }


        return "redirect:/basicHealthUnit-management";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @GetMapping(value = "/basicHealthUnit-management/systemUsers", produces = MediaType.TEXT_HTML_VALUE)
    public String getUBSsystemUsers(@RequestParam(value = "basicHealthUnit", required = false) Long basicHealthUnit,
                                    Model model) {

        List<UBSsystemUserDTO> ubsUsers = List.of();
        if (basicHealthUnit == null) {
            model.addAttribute("ubsUsers", ubsUsers);
            return "basicHealthUnitManagement/ubsFragments/emptySystemUsersUBSTable";
        }

        ubsUsers = basicHealthUnitService.findUBSsystemUsersByUBSid(basicHealthUnit);
        model.addAttribute("ubsUsers", ubsUsers);

        if (ubsUsers.isEmpty()) {
            return "basicHealthUnitManagement/ubsFragments/emptySystemUsersUBSTable";
        }

        return "basicHealthUnitManagement/ubsFragments/systemUsersUBSTable";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @GetMapping(value = "/basicHealthUnit-management/systemUser/search", produces = MediaType.TEXT_HTML_VALUE)
    public String getSystemUsersByLoggedUser(@RequestParam("systemUserSearch") String systemUser,
                                             Model model) {

        if (systemUser.isEmpty()) {
            model.addAttribute("users", List.of());
            return "basicHealthUnitManagement/ubsFragments/dropdownUserUBS";
        }

        final int THRESHOLD = 3;
        if (systemUser.length() < THRESHOLD) {
            model.addAttribute("users", List.of());
            return "basicHealthUnitManagement/ubsFragments/dropdownUserUBS";
        }

        model.addAttribute("users", systemUserService.findSystemUserByNameContaining(systemUser));

        return "basicHealthUnitManagement/ubsFragments/dropdownUserUBS";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @PostMapping("/basicHealthUnit-management/delete")
    public String deleteBasicHealthUnit(@RequestParam(value = "basicHealthUnit", required = false) Long basicHealthUnit,
                                        @AuthenticationPrincipal SystemUserDetails loggedUser,
                                        RedirectAttributes redirectAttributes) {

        if (basicHealthUnit == null) {
            return "redirect:/basicHealthUnit-management";
        }

        try {
            basicHealthUnitService.deleteBasicHealtUnit(basicHealthUnit, loggedUser);
            redirectAttributes.addFlashAttribute("message", "UBS deletada com sucesso. Todos os profissionais foram desvinculados.");
            redirectAttributes.addFlashAttribute("error", false);
            log.info("UBS deletada com sucesso.");
        } catch(Exception e) {
            redirectAttributes.addFlashAttribute("message", "Erro ao deletar UBS.");
            redirectAttributes.addFlashAttribute("error", true);
            log.error("Erro ao deletar UBS: {}", e.getMessage());
        }

        return "redirect:/basicHealthUnit-management";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @PostMapping("/basicHealthUnit-management/systemUser/delete")
    public String unlinkBasicHealthUnitSystemUser(@RequestParam("id") Long idSystemUser,
                                                  @RequestParam("basicHealthUnit") Long basicHealthUnit,
                                                  @AuthenticationPrincipal SystemUserDetails loggedUser,
                                                  Model model) {

        try {
            basicHealthUnitService.unlinkBasicHealthUnitSystemUser(idSystemUser, loggedUser);
            model.addAttribute("basicHealthUnit", basicHealthUnit);
            model.addAttribute("attach_message", "Usuário desvinculado com sucesso.");
            model.addAttribute("attach_error", false);
            log.info("SystemUser [id = {}] desvinculado da UBS [id = {}] com sucesso.", idSystemUser, basicHealthUnit);
        } catch(Exception e) {
            model.addAttribute("attach_message", "Erro ao desvincular usuário.");
            model.addAttribute("attach_error", true);
            log.error("Erro ao desvincular SystemUser [id = {}] deletado da UBS [id = {}]", idSystemUser, basicHealthUnit);
        }

        var ubsUsers = basicHealthUnitService.findUBSsystemUsersByUBSid(basicHealthUnit);
        model.addAttribute("ubsUsers", ubsUsers);

        if (ubsUsers.isEmpty()) {
            return "basicHealthUnitManagement/ubsFragments/emptySystemUsersUBSTable";
        }
        return "basicHealthUnitManagement/ubsFragments/systemUsersUBSTable";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @PostMapping(value = "/basicHealthUnit-management/systemUser/add", produces = MediaType.TEXT_HTML_VALUE)
    public String appendSystemUserToBasicHealthUnit(@RequestParam("systemUserSearch") String systemUserName,
                                                    @RequestParam("basicHealthUnit") Long basicHealthUnit,
                                                    @RequestParam("idSystemUser") Long idSystemUser,
                                                    Model model) {

        try {
            basicHealthUnitService.attachSystemUserToUBS(idSystemUser, basicHealthUnit);
            model.addAttribute("basicHealthUnit", basicHealthUnit);
            model.addAttribute("attach_message", "Usuário vinculado com sucesso.");
            model.addAttribute("attach_error", false);
            log.info("SystemUser [{}] vinculado da UBS [id = {}] com sucesso.", systemUserName, basicHealthUnit);
        } catch(Exception e) {
            model.addAttribute("attach_message", "Erro ao vincular usuário.");
            model.addAttribute("attach_error", true);
            log.error("Erro ao vincular SystemUser [{}] deletado da UBS [id = {}]", systemUserName, basicHealthUnit);
        }

        var ubsUsers = basicHealthUnitService.findUBSsystemUsersByUBSid(basicHealthUnit);
        model.addAttribute("ubsUsers", ubsUsers);

        if (ubsUsers.isEmpty()) {
            return "basicHealthUnitManagement/ubsFragments/emptySystemUsersUBSTable";
        }
        return "basicHealthUnitManagement/ubsFragments/systemUsersUBSTable";
    }

}
