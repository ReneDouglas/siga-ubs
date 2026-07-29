package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.dtos.PatientAppointmentsHistoryDTO;
import br.com.tecsus.sigaubs.dtos.PatientCommandDTO;
import br.com.tecsus.sigaubs.dtos.PatientSearchDTO;
import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.entities.Patient;
import br.com.tecsus.sigaubs.enums.Roles;
import br.com.tecsus.sigaubs.enums.SocialSituationRating;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.services.BasicHealthUnitService;
import br.com.tecsus.sigaubs.services.PatientService;
import br.com.tecsus.sigaubs.utils.DefaultValues;
import jakarta.validation.Valid;
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
import org.springframework.validation.BindingResult;

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

        Patient patient = loadPatientForForm(patientId, loggedUser, model);
        model.addAttribute("patient", patient);
        model.addAttribute("socialSituations", SocialSituationRating.getDescriptionSortedByRating());

        addPatientFormOptions(model, loggedUser);

        return "patientManagement/patient_management";
    }

    @PostMapping("/patient-management/create")
    public String registerPatient(@Valid @ModelAttribute PatientCommandDTO command,
                                  BindingResult bindingResult,
                                  @AuthenticationPrincipal SystemUserDetails loggedUser,
                                  Model model) {
        Patient patient = command.toFormPatient();
        if (bindingResult.hasErrors()) {
            model.addAttribute("patient", patient);
            model.addAttribute("message", firstValidationMessage(bindingResult));
            model.addAttribute("error", true);
            addPatientFormOptions(model, loggedUser);
            return "patientManagement/patientFragments/patient_form";
        }
        try {
            var resultado = patientService.registerPatient(command, loggedUser);
            if (resultado.sucesso()) {
                model.addAttribute("patient", new Patient());
                model.addAttribute("message", "Paciente cadastrado com sucesso.");
                model.addAttribute("error", false);
            } else {
                log.error("Erro ao cadastrar paciente: {}", resultado.mensagem());
                model.addAttribute("patient", patient);
                model.addAttribute("message", resultado.mensagem());
                model.addAttribute("error", true);
            }
        } catch (DataIntegrityViolationException e) {
            log.warn("Violação de integridade ao cadastrar paciente.");
            model.addAttribute("patient", patient);
            model.addAttribute("message", "CPF ou Cartão SUS já cadastrados no sistema.");
            model.addAttribute("error", true);
        } catch (Exception e) {
            log.error("Erro ao cadastrar paciente [{}].", e.getClass().getSimpleName());
            model.addAttribute("patient", patient);
            model.addAttribute("message", "Erro ao cadastrar paciente.");
            model.addAttribute("error", true);
        }
        addPatientFormOptions(model, loggedUser);
        return "patientManagement/patientFragments/patient_form";
    }

    @PostMapping("/patient-management/edit")
    public String patientToEdit(@RequestParam("id") Long patientId,
                                @AuthenticationPrincipal SystemUserDetails loggedUser,
                                Model model) {

        Patient patient = loadPatientForForm(patientId, loggedUser, model);
        model.addAttribute("patient", patient);
        addPatientFormOptions(model, loggedUser);
        return "patientManagement/patientFragments/patient_form";
    }

    @PostMapping("/patient-management/update")
    public String updatePatient(@Valid @ModelAttribute PatientCommandDTO command,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal SystemUserDetails loggedUser,
                                Model model) {

        Patient patient = command.toFormPatient();
        if (bindingResult.hasErrors()) {
            model.addAttribute("patient", patient);
            model.addAttribute("message", firstValidationMessage(bindingResult));
            model.addAttribute("error", true);
            addPatientFormOptions(model, loggedUser);
            return "patientManagement/patientFragments/patient_form";
        }
        try {
            var resultado = patientService.updatePatient(command, loggedUser);
            if (resultado.sucesso()) {
                model.addAttribute("patient", resultado.valor());
                model.addAttribute("message", "Paciente atualizado com sucesso.");
                model.addAttribute("error", false);
            } else {
                model.addAttribute("message", resultado.mensagem());
                model.addAttribute("error", true);
                addPatientFormOptions(model, loggedUser);
                log.error("Erro ao atualizar paciente: {}", resultado.mensagem());
                return "patientManagement/patientFragments/patient_form";
            }
        } catch (Exception e) {
            model.addAttribute("patient", patient);
            model.addAttribute("message", "Erro ao atualizar paciente.");
            model.addAttribute("error", true);
            addPatientFormOptions(model, loggedUser);
            log.error("Erro ao atualizar paciente [{}].", e.getClass().getSimpleName());
            return "patientManagement/patientFragments/patient_form";
        }
        return "patientManagement/patientFragments/patient_info";
    }

    @GetMapping("/patient-list")
    public String getPatientsPage(Model model,
                                  @Valid @ModelAttribute PatientSearchDTO patientSearch,
                                  BindingResult bindingResult,
                                  @AuthenticationPrincipal SystemUserDetails loggedUser,
                                  @RequestParam(value = "page", defaultValue = "0", required = false) int currentPage,
                                  @RequestParam(value = "pageSize", defaultValue = "" + DefaultValues.PAGE_SIZE, required = false) int pageSize,
                                  @RequestParam(value = "pagination", defaultValue = "false", required = false) boolean isPagination){

        int safePage = Math.max(0, currentPage);
        int safePageSize = Math.clamp(pageSize, 1, 100);
        Patient patient = patientSearch.toFilterEntity();
        Page<Patient> patientsPage = bindingResult.hasErrors()
                ? Page.empty(PageRequest.of(safePage, safePageSize))
                : patientService.findPatientsPage(patient, PageRequest.of(safePage, safePageSize), loggedUser);
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
                ? patientService.findPatientAppointmentsHistoryPage(patientId,
                        PageRequest.of(Math.max(0, currentPage), Math.clamp(pageSizeHistory, 1, 100)),
                        loggedUser)
                : new PageImpl<>(List.of(),
                        PageRequest.of(Math.max(0, currentPage), Math.clamp(pageSizeHistory, 1, 100)), 0);

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

    private Patient loadPatientForForm(Long patientId, SystemUserDetails loggedUser, Model model) {
        if (patientId == null) {
            return new Patient();
        }

        ResultadoOperacao<Patient> resultado = patientService.findPatientToEdit(patientId, loggedUser);
        if (resultado.sucesso()) {
            return resultado.valor();
        }

        model.addAttribute("message", resultado.mensagem());
        model.addAttribute("error", true);
        log.error("Erro ao carregar paciente [id={}]: {}", patientId, resultado.mensagem());
        return new Patient();
    }

    private String firstValidationMessage(BindingResult bindingResult) {
        return bindingResult.getAllErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Verifique os campos informados.");
    }
}
