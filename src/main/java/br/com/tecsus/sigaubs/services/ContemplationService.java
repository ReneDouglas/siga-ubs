package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.entities.Appointment;
import br.com.tecsus.sigaubs.entities.Contemplation;
import br.com.tecsus.sigaubs.entities.MedicalSlot;
import br.com.tecsus.sigaubs.enums.AppointmentStatus;
import br.com.tecsus.sigaubs.enums.Priorities;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.repositories.ContemplationRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.services.exceptions.CancelContemplationException;
import br.com.tecsus.sigaubs.services.exceptions.ConfirmContemplationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Service
public class ContemplationService {

    private static final Logger log = LoggerFactory.getLogger(ContemplationService.class);

    private final ContemplationRepository contemplationRepository;
    private final DateTimeFormatter formatter;
    private final MedicalSlotService medicalSlotService;
    private final AppointmentService appointmentService;
    private final AppointmentStatusHistoryService appointmentStatusHistoryService;

    @Autowired
    public ContemplationService(ContemplationRepository contemplationRepository, MedicalSlotService medicalSlotService, AppointmentService appointmentService, AppointmentStatusHistoryService appointmentStatusHistoryService) {
        this.contemplationRepository = contemplationRepository;
        this.medicalSlotService = medicalSlotService;
        this.appointmentStatusHistoryService = appointmentStatusHistoryService;
        this.formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        this.appointmentService = appointmentService;
    }

    public Page<Contemplation> findContemplationsByUBSAndSpecialty(ProcedureType type,
                                                                   Long ubsId,
                                                                   Long specialtyId,
                                                                   String referenceMonth,
                                                                   String status,
                                                                   Pageable page) {
        YearMonth yearMonth = null;

        if (referenceMonth != null && !referenceMonth.isEmpty()) {
            yearMonth = YearMonth.parse(referenceMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
        }

        return contemplationRepository
                .findConsultationsByUBSAndSpecialtyPaginated(type,
                        ubsId,
                        specialtyId,
                        yearMonth,
                        status == null || status.isEmpty() ? null : AppointmentStatus.getByDescription(status),
                        page);
    }

    @Transactional(readOnly = true)
    public Contemplation loadContemplatedById(Long contemplationId) {

        Contemplation contemplated = contemplationRepository.loadFetchedContemplationById(contemplationId);
        var statusList = appointmentStatusHistoryService.findAllAppointmentHistory(contemplated.getAppointment());
        contemplated.getAppointment().setAppointmentStatusHistory(statusList);

        return contemplated;
    }

    @Transactional
    public void cancelContemplationByAdmin(Long contemplatedId, String reason, SystemUserDetails loggedUser) throws CancelContemplationException {

        Contemplation contemplated = contemplationRepository.getReferenceById(contemplatedId);
        contemplated.getAppointment().setStatus(AppointmentStatus.CONTEMPLACAO_CANCELADA);
        contemplated.setUpdateUser(loggedUser.getName());
        contemplated.setUpdateDate(LocalDateTime.now());

        if (contemplated.isEmptyObservation()) {
            contemplated.setObservation("Cancelado por " + loggedUser.getName() + " em " + LocalDateTime.now().format(formatter) + " -- Motivo: " + reason);
        } else {
            contemplated.setObservation(contemplated.getObservation() + " -- Cancelado por " + loggedUser.getName() + " em " + LocalDateTime.now().format(formatter) + " -- Motivo: " + reason);
        }

        contemplationRepository.save(contemplated);
        appointmentStatusHistoryService.registerAppointmentStatusHistory(contemplated.getAppointment(), loggedUser.getName());

        log.info("Recuperando slot disponível.");
        medicalSlotService.addSlot(contemplated.getMedicalSlot());

    }

    @Transactional
    public void confirmContemplationByAdmin(Long contemplationId, SystemUserDetails loggedUser) throws ConfirmContemplationException {

        Contemplation contemplated = contemplationRepository.getReferenceById(contemplationId);
        contemplated.getAppointment().setStatus(AppointmentStatus.PRESENCA_CONFIRMADA);
        contemplated.setUpdateUser(loggedUser.getName());
        contemplated.setUpdateDate(LocalDateTime.now());
        contemplated.setObservation("Confirmado por " + loggedUser.getName() + " em " + LocalDateTime.now().format(formatter));

        contemplationRepository.save(contemplated);
        appointmentStatusHistoryService.registerAppointmentStatusHistory(contemplated.getAppointment(), loggedUser.getName());
    }

    @Transactional
    public void contemplateAppointmentByAdmin(Long appointmentId, String reason, Long medicalSlotId, SystemUserDetails loggedUser) {

        Appointment appt = appointmentService.findReferenceById(appointmentId);
        appt.setStatus(AppointmentStatus.PRESENCA_CONFIRMADA);

        MedicalSlot medicalSlot = new MedicalSlot();
        medicalSlot.setId(medicalSlotId);

        log.info("Removendo slot disponível.");
        medicalSlot = medicalSlotService.removeSlot(medicalSlot);

        Contemplation contemplation = new Contemplation();

        contemplation.setContemplationDate(LocalDateTime.now());
        contemplation.setContemplatedBy(Priorities.ADMINISTRATIVO);
        contemplation.setCreationDate(LocalDateTime.now());
        contemplation.setCreationUser(loggedUser.getUsername());
        contemplation.setAppointment(appt);
        contemplation.setMedicalSlot(medicalSlot);
        contemplation.setObservation("Paciente contemplado por " + loggedUser.getName() + " em " + LocalDateTime.now().format(formatter) + " -- Motivo: " + reason);

        log.info("Salvando contemplação via administrativo.");
        appt = appointmentService.updateAppointment(appt);
        contemplationRepository.save(contemplation);
        appointmentStatusHistoryService.registerAppointmentStatusHistory(appt, loggedUser.getName());

    }

    @Transactional
    public Contemplation registerContemplation(Contemplation contemplation) {
        return contemplationRepository.save(contemplation);
    }

}
