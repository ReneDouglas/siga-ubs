package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.dtos.PatientAppointmentsHistoryDTO;
import br.com.tecsus.sigaubs.entities.Patient;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.enums.SocialSituationRating;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.services.BasicHealthUnitService;
import br.com.tecsus.sigaubs.services.PatientService;
import br.com.tecsus.sigaubs.utils.DefaultValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class PatientController {

    private static final Logger log = LoggerFactory.getLogger(PatientController.class);

    private final PatientService patientService;
    private final BasicHealthUnitService basicHealthUnitService;

    @Autowired
    public PatientController(PatientService patientService, BasicHealthUnitService basicHealthUnitService) {
        this.patientService = patientService;
        this.basicHealthUnitService = basicHealthUnitService;
    }

    @GetMapping("/patient-management")
    public String getPatientInsertPage(Model model,
            @RequestParam(value = "id", required = false) Long patientId,
            @AuthenticationPrincipal SystemUserDetails loggedUser) {

        Patient patient = patientId != null ? patientService.findPatientToEdit(patientId, loggedUser) : new Patient();
        model.addAttribute("patient", patient);
        model.addAttribute("socialSituations", SocialSituationRating.getDescriptionSortedByRating());

        addPatientFormOptions(model, loggedUser);

        return "patientManagement/patient_management";
    }

    @PostMapping("/patient-management/create")
    public String registerPatient(@ModelAttribute Patient patient,
                                  @AuthenticationPrincipal SystemUserDetails loggedUser,
                                  Model model) {
        try {
            patientService.registerPatient(patient, loggedUser);
            model.addAttribute("patient", new Patient());
            model.addAttribute("message", "Paciente cadastrado com sucesso.");
            model.addAttribute("error", false);
        } catch (DataIntegrityViolationException e) {
            log.error("Violação de integridade [Paciente]: {}", e.getMessage());
            model.addAttribute("patient", patient);
            model.addAttribute("message", "CPF ou Cartão SUS já cadastrados no sistema.");
            model.addAttribute("error", true);
        } catch (Exception e) {
            log.error("Erro ao cadastrar paciente: {}", e.getMessage());
            model.addAttribute("patient", patient);
            model.addAttribute("message", "Erro ao cadastrar paciente.");
            model.addAttribute("error", true);
        }
        addPatientFormOptions(model, loggedUser);
        return "patientManagement/patientFragments/patient_form";
    }

    @PostMapping("/patient-management/edit")
    public String patientToEdit(@ModelAttribute Patient patient,
                                @AuthenticationPrincipal SystemUserDetails loggedUser,
                                Model model) {

        model.addAttribute("patient", patient);
        addPatientFormOptions(model, loggedUser);
        return "patientManagement/patientFragments/patient_form";
    }

    @PostMapping("/patient-management/update")
    public String updatePatient(@ModelAttribute Patient patient,
                                @AuthenticationPrincipal SystemUserDetails loggedUser,
                                Model model) {

        try {
            Patient updatedPatient = patientService.updatePatient(patient, loggedUser);
            model.addAttribute("patient", updatedPatient);
            model.addAttribute("message", "Paciente atualizado com sucesso.");
            model.addAttribute("error", false);
        } catch (Exception e) {
            model.addAttribute("message", "Erro ao atualizar paciente.");
            model.addAttribute("error", true);
            addPatientFormOptions(model, loggedUser);
            log.error("Erro ao atualizar paciente: {}", e.getMessage());
            return "patientManagement/patientFragments/patient_form";
        }
        return "patientManagement/patientFragments/patient_info";
    }

    @GetMapping("/patient-list")
    public String getPatientsPage(Model model,
                                  @ModelAttribute Patient patient,
                                  @AuthenticationPrincipal SystemUserDetails loggedUser,
                                  @RequestParam(value = "page", defaultValue = "0", required = false) int currentPage,
                                  @RequestParam(value = "pageSize", defaultValue = "" + DefaultValues.PAGE_SIZE, required = false) int pageSize,
                                  @RequestParam(value = "pagination", defaultValue = "false", required = false) boolean isPagination){

        Page<Patient> patientsPage = patientService.findPatientsPage(patient, PageRequest.of(currentPage, pageSize), loggedUser);
        model.addAttribute("patientsPage", patientsPage);
        model.addAttribute("patientHistoryPage", new PageImpl<>(List.of(), PageRequest.of(0, DefaultValues.PAGE_SIZE), 0));
        model.addAttribute("patient", patient);

        if (!isPagination) {
            return "patientManagement/patient_list";
        }
        return "patientManagement/patientFragments/patient_datatable";

    }

    @GetMapping("/patient-list/history")
    public String getPatientAppointmentsHistory(Model model,
                                                @AuthenticationPrincipal SystemUserDetails loggedUser,
                                                @RequestParam(value = "id", required = false) Long patientId,
                                                @RequestParam(value = "page", defaultValue = "0", required = false) int currentPage,
                                                @RequestParam(value = "pageSizeHistory", defaultValue = "" + DefaultValues.PAGE_SIZE, required = false) int pageSizeHistory,
                                                @RequestParam(value = "pagination", defaultValue = "false", required = false) boolean isPagination) {

        Page<PatientAppointmentsHistoryDTO> patientHistoryPage = patientId != null
                ? patientService.findPatientAppointmentsHistoryPage(patientId, PageRequest.of(currentPage, pageSizeHistory),
                        loggedUser)
                : new PageImpl<>(List.of(), PageRequest.of(currentPage, pageSizeHistory), 0);

        model.addAttribute("patientHistoryPage", patientHistoryPage);
        model.addAttribute("patientHistoryId", patientId);
        return "patientManagement/patientFragments/patient_history";
    }

    @GetMapping(value = "/patient-list/search", produces = MediaType.TEXT_HTML_VALUE)
    public String searchPatient(@RequestParam("name") String patient,
                                @RequestParam(value = "autocomplete", defaultValue = "false", required = false) boolean autocomplete,
                                @AuthenticationPrincipal SystemUserDetails loggedUser,
                                Model model) {

        if (patient.isEmpty()) {
            model.addAttribute("patients", List.of());
            if (autocomplete) {
                return "patientManagement/patientFragments/patient_search_autocomplete";
            }
            return "patientManagement/patientFragments/patient_search_dropdown";
        }

        final int THRESHOLD = 4;
        if (patient.length() < THRESHOLD) {
            model.addAttribute("patients", List.of());
            if (autocomplete) {
                return "patientManagement/patientFragments/patient_search_autocomplete";
            }
            return "patientManagement/patientFragments/patient_search_dropdown";
        }

        model.addAttribute("patients", patientService.searchNativePatients(patient, loggedUser));

        if (autocomplete) {
            return "patientManagement/patientFragments/patient_search_autocomplete";
        }

        return "patientManagement/patientFragments/patient_search_dropdown";
    }

    @GetMapping("/patient-management/cancel")
    public String cancelPatientEdit() {
        return "redirect:/patient-management";
    }

    @GetMapping("/patient-list/clear")
    public String clearPatientsPage() {
        return "redirect:/patient-list";
    }

    @GetMapping("/patient-list/edit")
    public String editSelectedPatient(@RequestParam(value = "id") Long patientId) throws RuntimeException{
        return "redirect:/patient-management?id=" + patientId;
    }

    private void addPatientFormOptions(Model model, SystemUserDetails loggedUser) {
        boolean canSelectBasicHealthUnit = loggedUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Roles.ROLE_ADMIN.toString())
                        || a.getAuthority().equals(Roles.ROLE_SMS.toString()));
        if (canSelectBasicHealthUnit) {
            model.addAttribute("basicHealthUnits", basicHealthUnitService.findAllUBS());
        } else if (loggedUser.getBasicHealthUnitId() != null) {
            model.addAttribute("systemUserUBS", basicHealthUnitService.findSystemUserUBS(loggedUser.getBasicHealthUnitId()));
        }
    }
}
