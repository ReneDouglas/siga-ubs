package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO;
import br.com.tecsus.sigaubs.entities.*;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.services.*;
import br.com.tecsus.sigaubs.services.exceptions.CancelContemplationException;
import br.com.tecsus.sigaubs.utils.DefaultValues;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
@SessionScope
public class QueueController {

    private final AppointmentService appointmentService;
    private final MedicalSlotService medicalSlotService;
    private final BasicHealthUnitService basicHealthUnitService;
    private final List<BasicHealthUnit> basicHealthUnits;
    private final List<Specialty> specialties;
    private final ContemplationService contemplationService;

    @Autowired
    public QueueController(BasicHealthUnitService basicHealthUnitService, SpecialtyService specialtyService,
            AppointmentService appointmentService, MedicalSlotService medicalSlotService,
            ContemplationService contemplationService) {
        this.basicHealthUnitService = basicHealthUnitService;
        this.basicHealthUnits = basicHealthUnitService.findAllUBS();
        this.specialties = specialtyService.findSpecialties();
        this.appointmentService = appointmentService;
        this.medicalSlotService = medicalSlotService;
        this.contemplationService = contemplationService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @GetMapping("/queue-management")
    public String getQueuePage(Model model) {

        model.addAttribute("basicHealthUnits", this.basicHealthUnits);
        model.addAttribute("specialties", this.specialties);
        model.addAttribute("consultasPage",
                new PageImpl<Contemplation>(List.of(), PageRequest.of(0, DefaultValues.PAGE_SIZE), 0));
        model.addAttribute("examesPage",
                new PageImpl<Contemplation>(List.of(), PageRequest.of(0, DefaultValues.PAGE_SIZE), 0));
        model.addAttribute("cirurgiasPage",
                new PageImpl<Contemplation>(List.of(), PageRequest.of(0, DefaultValues.PAGE_SIZE), 0));
        model.addAttribute("hide", "hidden");

        return "queueManagement/queue-management";
    }

    @GetMapping("/queue-management/v2")
    public String getQueuePageV2(Model model,
            @RequestParam(required = false) Long basicHealthUnit,
            @RequestParam(required = false) Long specialty,
            @RequestParam(required = false) Long medicalProcedure,
            @RequestParam(required = false) String procedureType,
            @AuthenticationPrincipal SystemUserDetails loggedUser) {

        boolean isAdminOrSms = loggedUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SMS"));

        if (!isAdminOrSms) {
            basicHealthUnit = loggedUser.getBasicHealthUnitId();
            model.addAttribute("basicHealthUnits",
                    basicHealthUnit != null
                            ? List.of(basicHealthUnitService.findSystemUserUBS(basicHealthUnit))
                            : List.of());
        } else {
            model.addAttribute("basicHealthUnits", this.basicHealthUnits);
        }

        model.addAttribute("specialties", this.specialties);

        var queuePage = appointmentService
                .findOpenAppointmentsQueuePaginatedV2(
                        basicHealthUnit,
                        specialty,
                        medicalProcedure,
                        PageRequest.of(0, DefaultValues.PAGE_SIZE));

        model.addAttribute("queuePage", queuePage);
        model.addAttribute("selectedUBS", basicHealthUnit);
        model.addAttribute("selectedSpecialty", specialty);
        model.addAttribute("selectedMedicalProcedure", medicalProcedure);
        model.addAttribute("selectedProcedureType", procedureType);
        model.addAttribute("procedures", List.of());

        return "queueManagement/queue-management-v2";

    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @GetMapping("/queue-management/search")
    public String searchOpenAppointmentsQueue(@RequestParam Long basicHealthUnit,
            @RequestParam Long specialty,
            Model model) {

        var consultas = appointmentService
                .findOpenAppointmentsQueuePaginated(
                        ProcedureType.CONSULTA,
                        basicHealthUnit,
                        specialty,
                        PageRequest.of(0, DefaultValues.PAGE_SIZE));
        var exames = appointmentService
                .findOpenAppointmentsQueuePaginated(
                        ProcedureType.EXAME,
                        basicHealthUnit,
                        specialty,
                        PageRequest.of(0, DefaultValues.PAGE_SIZE));
        var cirurgias = appointmentService
                .findOpenAppointmentsQueuePaginated(
                        ProcedureType.CIRURGIA,
                        basicHealthUnit,
                        specialty,
                        PageRequest.of(0, DefaultValues.PAGE_SIZE));

        var totalProceduresType = appointmentService.findProcedureTypeTotal(basicHealthUnit, specialty);
        var totalMedicalProcedures = appointmentService.findMedicalProceduresTotal(basicHealthUnit, specialty);

        model.addAttribute("selectedUBS", basicHealthUnit);
        model.addAttribute("basicHealthUnits", this.basicHealthUnits);
        model.addAttribute("selectedSpecialty", specialty);
        model.addAttribute("specialties", this.specialties);
        model.addAttribute("consultasPage", consultas);
        model.addAttribute("examesPage", exames);
        model.addAttribute("cirurgiasPage", cirurgias);
        model.addAttribute("totalProceduresType", totalProceduresType);
        model.addAttribute("totalMedicalProcedures", totalMedicalProcedures);
        model.addAttribute("hide", "hidden");

        return "queueManagement/queue-management";
    }

    @GetMapping("/queue-management/v2/search")
    public String searchOpenAppointmentsQueueV2(@RequestParam(required = false) Long basicHealthUnit,
            @RequestParam(required = false) Long specialty,
            @RequestParam(required = false) Long medicalProcedure,
            @RequestParam(required = false) String procedureType,
            Model model,
            @AuthenticationPrincipal SystemUserDetails loggedUser) {

        boolean isAdminOrSms = loggedUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SMS"));

        if (!isAdminOrSms) {
            basicHealthUnit = loggedUser.getBasicHealthUnitId();
        }

        var queuePage = appointmentService
                .findOpenAppointmentsQueuePaginatedV2(
                        basicHealthUnit,
                        specialty,
                        medicalProcedure,
                        PageRequest.of(0, DefaultValues.PAGE_SIZE));

        model.addAttribute("selectedUBS", basicHealthUnit);
        model.addAttribute("basicHealthUnits", this.basicHealthUnits);
        model.addAttribute("selectedSpecialty", specialty);
        model.addAttribute("selectedMedicalProcedure", medicalProcedure);
        model.addAttribute("selectedProcedureType", procedureType);
        model.addAttribute("specialties", this.specialties);
        model.addAttribute("queuePage", queuePage);

        return "queueManagement/queueFragments/queue-tabs-v2 :: queue-datatable";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @GetMapping("/queue-management/paginated")
    public String getOpenAppointmentsQueuePaginated(Model model,
            @RequestParam(value = "page", defaultValue = "0", required = false) int currentPage,
            @RequestParam(value = "consultasPageSize", defaultValue = ""
                    + DefaultValues.PAGE_SIZE, required = false) int consultasPageSize,
            @RequestParam(value = "examesPageSize", defaultValue = ""
                    + DefaultValues.PAGE_SIZE, required = false) int examesPageSize,
            @RequestParam(value = "cirurgiasPageSize", defaultValue = ""
                    + DefaultValues.PAGE_SIZE, required = false) int cirurgiasPageSize,
            @RequestParam(value = "ubs") Long ubs,
            @RequestParam(value = "specialty") Long specialty,
            @RequestParam(value = "type") String procedureType) {

        model.addAttribute("selectedUBS", ubs);
        model.addAttribute("selectedSpecialty", specialty);

        if (procedureType.equals(ProcedureType.CONSULTA.toString())) {
            model.addAttribute("consultasPage", appointmentService
                    .findOpenAppointmentsQueuePaginated(
                            ProcedureType.CONSULTA,
                            ubs,
                            specialty,
                            PageRequest.of(currentPage, consultasPageSize)));
            return "queueManagement/queueFragments/queue-tabs :: consultas-datatable";
        } else if (procedureType.equals(ProcedureType.EXAME.toString())) {
            model.addAttribute("examesPage", appointmentService
                    .findOpenAppointmentsQueuePaginated(
                            ProcedureType.EXAME,
                            ubs,
                            specialty,
                            PageRequest.of(currentPage, examesPageSize)));
            return "queueManagement/queueFragments/queue-tabs :: exames-datatable";
        } else if (procedureType.equals(ProcedureType.CIRURGIA.toString())) {
            model.addAttribute("cirurgiasPage", appointmentService
                    .findOpenAppointmentsQueuePaginated(
                            ProcedureType.CIRURGIA,
                            ubs,
                            specialty,
                            PageRequest.of(currentPage, cirurgiasPageSize)));
            return "queueManagement/contemplationFragments/queue-tabs :: cirurgias-datatable";

        }
        return "queueManagement/queue-management";
    }

    @GetMapping("/queue-management/v2/paginated")
    public String getOpenAppointmentsQueuePaginatedV2(Model model,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "" + DefaultValues.PAGE_SIZE, required = false) int size,
            @RequestParam(value = "ubs", required = false) Long ubs,
            @RequestParam(value = "specialty", required = false) Long specialty,
            @RequestParam(value = "medicalProcedure", required = false) Long medicalProcedure,
            @RequestParam(value = "procedureType", required = false) String procedureType,
            @AuthenticationPrincipal SystemUserDetails loggedUser) {

        boolean isAdminOrSms = loggedUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SMS"));

        if (!isAdminOrSms) {
            ubs = loggedUser.getBasicHealthUnitId();
        }

        model.addAttribute("selectedUBS", ubs);
        model.addAttribute("selectedSpecialty", specialty);
        model.addAttribute("selectedMedicalProcedure", medicalProcedure);
        model.addAttribute("selectedProcedureType", procedureType);

        model.addAttribute("queuePage", appointmentService
                .findOpenAppointmentsQueuePaginatedV2(
                        ubs,
                        specialty,
                        medicalProcedure,
                        PageRequest.of(page, size)));

        return "queueManagement/queueFragments/queue-tabs-v2 :: queue-datatable";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @GetMapping("/queue-management/{id}/load")
    public String loadOpenAppointment(@PathVariable Long id,
            @RequestParam Long ubs,
            @RequestParam Long specialty,
            Model model) {

        Appointment appointment = appointmentService.findById(id);
        MedicalSlot medicalSlot = new MedicalSlot();
        medicalSlot.setMedicalProcedure(appointment.getMedicalProcedure());
        medicalSlot.setBasicHealthUnit(appointment.getPatient().getBasicHealthUnit());

        List<PatientOpenAppointmentDTO> patientOpenAppointments = appointmentService
                .findPatientOpenAppointments(appointment.getPatient().getId());
        patientOpenAppointments.removeIf(openAppt -> openAppt.appointmentId().equals(id));

        Optional<MedicalSlot> availableSlots = medicalSlotService.findAvailableSlotsV2(medicalSlot);
        int quantity = availableSlots.map(MedicalSlot::getCurrentSlots).orElse(0);

        model.addAttribute("selectedUBS", ubs);
        model.addAttribute("selectedSpecialty", specialty);
        model.addAttribute("appointment", appointment);
        model.addAttribute("isContemplated", false);
        model.addAttribute("availableSlots", quantity);
        model.addAttribute("medicalSlotId", medicalSlot.getId());
        model.addAttribute("patientOpenAppointments", patientOpenAppointments);

        return "queueManagement/queueFragments/patientAppointment-info :: patientAppointmentInfo";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @GetMapping("/queue-management/v2/{id}/load")
    public String loadOpenAppointmentV2(@PathVariable Long id,
            @RequestParam(required = false) Long ubs,
            @RequestParam(required = false) Long specialty,
            @RequestParam(required = false) Long medicalProcedure,
            @RequestParam(required = false) String procedureType,
            Model model) {

        Appointment appointment = appointmentService.findById(id);
        MedicalSlot medicalSlot = new MedicalSlot();
        medicalSlot.setMedicalProcedure(appointment.getMedicalProcedure());
        medicalSlot.setBasicHealthUnit(appointment.getPatient().getBasicHealthUnit());

        List<PatientOpenAppointmentDTO> patientOpenAppointments = appointmentService
                .findPatientOpenAppointments(appointment.getPatient().getId());
        patientOpenAppointments.removeIf(openAppt -> openAppt.appointmentId().equals(id));

        Optional<MedicalSlot> availableSlots = medicalSlotService.findAvailableSlotsV2(medicalSlot);
        int quantity = availableSlots.map(MedicalSlot::getCurrentSlots).orElse(0);

        model.addAttribute("selectedUBS", ubs);
        model.addAttribute("selectedSpecialty", specialty);
        model.addAttribute("selectedMedicalProcedure", medicalProcedure);
        model.addAttribute("selectedProcedureType", procedureType);
        model.addAttribute("appointment", appointment);
        model.addAttribute("isContemplated", false);
        model.addAttribute("availableSlots", quantity);
        model.addAttribute("medicalSlotId", availableSlots.map(MedicalSlot::getId).orElse(null));
        model.addAttribute("patientOpenAppointments", patientOpenAppointments);

        return "queueManagement/queueFragments/patientAppointment-info :: patientAppointmentInfo";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @PostMapping(value = "/queue-management/contemplate")
    public String contemplateByAdmin(@RequestParam String reason,
            @RequestParam Long ubs,
            @RequestParam Long specialty,
            @RequestParam Long appointmentId,
            @RequestParam Long medicalSlotId,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Iniciando contemplação de paciente.");
            contemplationService.contemplateAppointmentByAdmin(appointmentId, reason, medicalSlotId, loggedUser);
            redirectAttributes.addFlashAttribute("error", false);
            redirectAttributes.addFlashAttribute("message", "Paciente contemplado com sucesso.");
            log.info("Paciente [Appoinment.id={}] contemplado com sucesso pelo usuário[nome={}].", appointmentId,
                    loggedUser.getName());
        } catch (CancelContemplationException e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("message", "Erro ao cancelar contemplação.");
            log.info("Erro ao contemplar paciente [Appoinment.id={}][SystemUser={}].", appointmentId,
                    loggedUser.getName());
        }

        return "redirect:/queue-management/search?basicHealthUnit=" + ubs + "&specialty=" + specialty;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SMS')")
    @PostMapping(value = "/queue-management/v2/contemplate")
    public String contemplateByAdminV2(@RequestParam String reason,
            @RequestParam(required = false) Long ubs,
            @RequestParam(required = false) Long specialty,
            @RequestParam(required = false) Long medicalProcedure,
            @RequestParam(required = false) String procedureType,
            @RequestParam Long appointmentId,
            @RequestParam Long medicalSlotId,
            @AuthenticationPrincipal SystemUserDetails loggedUser,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Iniciando contemplação de paciente.");
            // contemplationService.contemplateAppointmentByAdmin(appointmentId, reason,
            // medicalSlotId, loggedUser);
            redirectAttributes.addFlashAttribute("error", false);
            redirectAttributes.addFlashAttribute("message", "Paciente contemplado com sucesso.");
            log.info("Paciente [Appoinment.id={}] contemplado com sucesso pelo usuário[nome={}].", appointmentId,
                    loggedUser.getName());
        } catch (CancelContemplationException e) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("message", "Erro ao cancelar contemplação.");
            log.info("Erro ao contemplar paciente [Appoinment.id={}][SystemUser={}].", appointmentId,
                    loggedUser.getName());
        }

        UriComponentsBuilder redirectUrl = UriComponentsBuilder.fromPath("/queue-management/v2");
        if (ubs != null)
            redirectUrl.queryParam("basicHealthUnit", ubs);
        if (specialty != null)
            redirectUrl.queryParam("specialty", specialty);
        if (medicalProcedure != null)
            redirectUrl.queryParam("medicalProcedure", medicalProcedure);
        if (procedureType != null && !procedureType.isEmpty())
            redirectUrl.queryParam("procedureType", procedureType);
        return "redirect:" + redirectUrl.build().toUriString();
    }

    @GetMapping(value = "/queue-management/v2/procedures", produces = MediaType.TEXT_HTML_VALUE)
    public String loadProcedure(@RequestParam("procedureType") String procedureType,
            @RequestParam("specialty") Long specialtyId,
            Model model) {

        List<MedicalProcedure> procedures = loadProcedures(procedureType, specialtyId);
        model.addAttribute("procedures", procedures);

        return "queueManagement/queueFragments/medicalProcedures :: medicalProcedures";
    }

    @GetMapping("/queue-management/v2/clear")
    public String clearSearch() {
        return "redirect:/queue-management/v2";
    }

    private List<MedicalProcedure> loadProcedures(String procedureType, Long specialtyId) {
        if (procedureType.equals(ProcedureType.CONSULTA.toString())) {
            return appointmentService.findBySpecialtyIdAndProcedureType(specialtyId, ProcedureType.CONSULTA);
        } else if (procedureType.equals(ProcedureType.EXAME.toString())) {
            return appointmentService.findBySpecialtyIdAndProcedureType(specialtyId, ProcedureType.EXAME);
        } else {
            return appointmentService.findBySpecialtyIdAndProcedureType(specialtyId, ProcedureType.CIRURGIA);
        }
    }

}
