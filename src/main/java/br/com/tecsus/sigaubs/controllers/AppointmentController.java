package br.com.tecsus.sigaubs.controllers;

import br.com.tecsus.sigaubs.entities.Appointment;
import br.com.tecsus.sigaubs.entities.MedicalProcedure;
import br.com.tecsus.sigaubs.entities.Patient;
import br.com.tecsus.sigaubs.enums.Priorities;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.services.AppointmentService;
import br.com.tecsus.sigaubs.services.PatientService;
import br.com.tecsus.sigaubs.services.SpecialtyService;
import br.com.tecsus.sigaubs.services.exceptions.AppointmentRegistrationFailureException;
import br.com.tecsus.sigaubs.services.exceptions.CancelAppointmentException;
import br.com.tecsus.sigaubs.services.exceptions.DuplicateAppointmentRegistrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@SessionScope
public class AppointmentController {

    private static final Logger log = LoggerFactory.getLogger(AppointmentController.class);

    private final PatientService patientService;
    private final SpecialtyService specialtyService;
    private final AppointmentService appointmentService;

    public AppointmentController(PatientService patientService, SpecialtyService specialtyService, AppointmentService appointmentService) {
        this.patientService = patientService;
        this.specialtyService = specialtyService;
        this.appointmentService = appointmentService;
    }

    @GetMapping("/appointment-management")
    public String getAppointmentPage(Model model) {

        Appointment appointment = new Appointment();
        appointment.setPatient(new Patient());
        appointment.setMedicalProcedure(new MedicalProcedure());

        model.addAttribute("patients", List.of());
        model.addAttribute("appointment", appointment);
        model.addAttribute("patientOpenAppointments", List.of());
        model.addAttribute("specialties", specialtyService.findSpecialties());
        model.addAttribute("loaded", false);

        return "appointmentManagement/appointment_management";
    }


    @GetMapping(value = "/appointment-management/load", produces = MediaType.TEXT_HTML_VALUE)
    public String loadPatient(@RequestParam("id") Long idPatient,
                              Model model,
                              @AuthenticationPrincipal SystemUserDetails loggedUser) {

        Appointment appointment = new Appointment();
        appointment.setPatient(patientService.findByIdAndUBS(idPatient, loggedUser.getBasicHealthUnitId()));

        model.addAttribute("patients", List.of());
        model.addAttribute("appointment", appointment);
        model.addAttribute("specialties", specialtyService.findSpecialties());
        model.addAttribute("patientOpenAppointments", appointmentService.findPatientOpenAppointments(idPatient));
        model.addAttribute("loaded", true);
        return "appointmentManagement/appointment_management";
    }

    @GetMapping(value = "/appointment-management/procedures", produces = MediaType.TEXT_HTML_VALUE)
    public String loadPrioritiesByProcedure(@RequestParam("procedureType") String procedureType,
                                            @RequestParam("specialty") Long specialtyId,
                                            @RequestParam(name = "slot", required = false, defaultValue = "false") boolean fromMedicalSlotPage,
                                            Model model) {

        List<MedicalProcedure> procedures;
        List<Priorities> priorities = List.of(Priorities.ELETIVO, Priorities.URGENCIA);

        if (procedureType.equals(ProcedureType.CONSULTA.toString())) {
            procedures = appointmentService.findBySpecialtyIdAndProcedureType(specialtyId, ProcedureType.CONSULTA);
            priorities = List.of(Priorities.ELETIVO, Priorities.URGENCIA, Priorities.RETORNO);
            model.addAttribute("isConsultation", true);
        } else if (procedureType.equals(ProcedureType.EXAME.toString())){
            procedures = appointmentService.findBySpecialtyIdAndProcedureType(specialtyId, ProcedureType.EXAME);
        } else {
            procedures = appointmentService.findBySpecialtyIdAndProcedureType(specialtyId, ProcedureType.CIRURGIA);
        }

        model.addAttribute("procedures", procedures);
        model.addAttribute("priorities", priorities);
        return "appointmentManagement/appointmentFragments/procedure_priority_dropdown";
    }

    @PostMapping("/appointment-management/create")
    public String registerAppointmentSolicitation(@ModelAttribute Appointment appointment,
                                            @AuthenticationPrincipal SystemUserDetails loggedUser,
                                            RedirectAttributes redirectAttributes) {

        try{
            appointmentService.registerAppointment(appointment, loggedUser);
            redirectAttributes.addFlashAttribute("message", "Marcação agendada com sucesso.");
            redirectAttributes.addFlashAttribute("error", false);
            log.info("Marcação agendada com sucesso.");
        } catch (AppointmentRegistrationFailureException e) {
            log.error("Erro ao registrar consulta: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("message", "Erro ao agendar marcação. Tente novamente ou contate o TI.");
            redirectAttributes.addFlashAttribute("error", true);
        } catch (DuplicateAppointmentRegistrationException e) {
            log.error("Marcação de consulta duplicada: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("message", "Existe uma marcação em aberto para este procedimento.");
            redirectAttributes.addFlashAttribute("error", true);
        }

        return "redirect:/appointment-management/load?id=" + appointment.getPatient().getId();
    }

    @PutMapping("/appointment-management/{id}/cancel")
    public String cancelAppointmentSolicitation(@PathVariable("id") Long apptSolicitationId,
                                                @RequestParam("patientId") Long patientId,
                                                @AuthenticationPrincipal SystemUserDetails loggedUser,
                                                RedirectAttributes redirectAttributes) {

        try {
            appointmentService.cancelSolicitation(apptSolicitationId, loggedUser);
            redirectAttributes.addFlashAttribute("message", "Marcação cancelada com sucesso.");
            redirectAttributes.addFlashAttribute("error", false);
            log.info("Marcação cancelada com sucesso.");
        } catch (CancelAppointmentException e) {
            log.error("Não foi possível cancelar a marcação de consulta: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("message", "Não foi possível cancelar a marcação. Contate o TI.");
            redirectAttributes.addFlashAttribute("error", true);
        }
        return "redirect:/appointment-management/load?id=" + patientId;
    }

}
