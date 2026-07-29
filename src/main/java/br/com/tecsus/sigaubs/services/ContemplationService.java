package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.ResultadoOperacao;
import br.com.tecsus.sigaubs.entities.Appointment;
import br.com.tecsus.sigaubs.entities.Contemplation;
import br.com.tecsus.sigaubs.entities.MedicalSlot;
import br.com.tecsus.sigaubs.enums.AppointmentStatus;
import br.com.tecsus.sigaubs.enums.Priorities;
import br.com.tecsus.sigaubs.enums.ProcedureType;
import br.com.tecsus.sigaubs.repositories.ContemplationRepository;
import br.com.tecsus.sigaubs.security.SystemUserDetails;
import br.com.tecsus.sigaubs.security.AuthorizationScopeService;
import br.com.tecsus.sigaubs.utils.ContemplationLimits;
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
    private static final String AUTOMATED_JOB_USER = "ROTINA";

    private final ContemplationRepository contemplationRepository;
    private final DateTimeFormatter formatter;
    private final MedicalSlotService medicalSlotService;
    private final AppointmentService appointmentService;
    private final AppointmentStatusHistoryService appointmentStatusHistoryService;
    private final AuthorizationScopeService authorizationScopeService;

    @Autowired
    public ContemplationService(ContemplationRepository contemplationRepository,
            MedicalSlotService medicalSlotService,
            AppointmentService appointmentService,
            AppointmentStatusHistoryService appointmentStatusHistoryService,
            AuthorizationScopeService authorizationScopeService) {
        this.contemplationRepository = contemplationRepository;
        this.medicalSlotService = medicalSlotService;
        this.appointmentStatusHistoryService = appointmentStatusHistoryService;
        this.formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        this.appointmentService = appointmentService;
        this.authorizationScopeService = authorizationScopeService != null
                ? authorizationScopeService
                : new AuthorizationScopeService();
    }

    ContemplationService(ContemplationRepository contemplationRepository,
            MedicalSlotService medicalSlotService,
            AppointmentService appointmentService,
            AppointmentStatusHistoryService appointmentStatusHistoryService) {
        this(contemplationRepository, medicalSlotService, appointmentService,
                appointmentStatusHistoryService, new AuthorizationScopeService());
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
                        status == null || status.isEmpty()
                                ? null
                                : AppointmentStatus.findByDescription(status).orElse(null),
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
    public ResultadoOperacao<Void> cancelContemplationByAdmin(Long contemplatedId, String reason,
            SystemUserDetails loggedUser) {

        Contemplation contemplated = contemplationRepository.findFetchedForUpdateById(contemplatedId);
        if (contemplated == null) {
            return ResultadoOperacao.falha("Contemplação não encontrada.");
        }
        authorizationScopeService.requireCurrentTenant(contemplated);
        authorizationScopeService.requireBasicHealthUnit(
                loggedUser, contemplated.getAppointment().getPatient().getBasicHealthUnit().getId());
        if (contemplated.getAppointment().getStatus() == AppointmentStatus.CONTEMPLACAO_CANCELADA) {
            return ResultadoOperacao.falha("A contemplação já foi cancelada.");
        }
        String normalizedReason = normalizeReason(reason);
        if (normalizedReason == null) {
            return invalidContemplationReason();
        }

        var slotResult = medicalSlotService.addSlot(contemplated.getMedicalSlot());
        if (slotResult.falhou()) {
            return ResultadoOperacao.falha(slotResult.mensagem());
        }

        contemplated.getAppointment().setStatus(AppointmentStatus.CONTEMPLACAO_CANCELADA);
        contemplated.setUpdateUser(loggedUser.getName());
        contemplated.setUpdateDate(LocalDateTime.now());

        if (contemplated.isEmptyObservation()) {
            contemplated.setObservation("Cancelado por " + loggedUser.getName() + " em "
                    + LocalDateTime.now().format(formatter) + " -- Motivo: " + normalizedReason);
        } else {
            contemplated.setObservation(truncate(contemplated.getObservation() + " -- Cancelado por "
                    + loggedUser.getName() + " em " + LocalDateTime.now().format(formatter)
                    + " -- Motivo: " + normalizedReason,
                    ContemplationLimits.MAXIMUM_OBSERVATION_LENGTH));
        }

        contemplationRepository.save(contemplated);
        appointmentStatusHistoryService.registerAppointmentStatusHistory(contemplated.getAppointment(), loggedUser.getName());

        return ResultadoOperacao.sucessoSemValor();

    }

    @Transactional
    public ResultadoOperacao<Void> confirmContemplationByAdmin(Long contemplationId, SystemUserDetails loggedUser) {

        Contemplation contemplated = contemplationRepository.findFetchedForUpdateById(contemplationId);
        if (contemplated == null) {
            return ResultadoOperacao.falha("Contemplação não encontrada.");
        }
        authorizationScopeService.requireCurrentTenant(contemplated);
        authorizationScopeService.requireBasicHealthUnit(
                loggedUser, contemplated.getAppointment().getPatient().getBasicHealthUnit().getId());
        if (contemplated.getAppointment().getStatus() != AppointmentStatus.PACIENTE_CONTEMPLADO) {
            return ResultadoOperacao.falha("A contemplação não pode ser confirmada no estado atual.");
        }

        contemplated.getAppointment().setStatus(AppointmentStatus.PRESENCA_CONFIRMADA);
        contemplated.setUpdateUser(loggedUser.getName());
        contemplated.setUpdateDate(LocalDateTime.now());
        contemplated.setObservation("Confirmado por " + loggedUser.getName() + " em " + LocalDateTime.now().format(formatter));

        contemplationRepository.save(contemplated);
        appointmentStatusHistoryService.registerAppointmentStatusHistory(contemplated.getAppointment(), loggedUser.getName());
        return ResultadoOperacao.sucessoSemValor();
    }

    @Transactional
    public ResultadoOperacao<Void> contemplateAppointmentByAdmin(Long appointmentId, String reason,
            Long medicalSlotId, SystemUserDetails loggedUser) {

        String normalizedReason = normalizeReason(reason);
        if (normalizedReason == null) {
            return invalidContemplationReason();
        }
        return contemplateAtomically(
                appointmentId,
                medicalSlotId,
                Priorities.ADMINISTRATIVO,
                AppointmentStatus.PRESENCA_CONFIRMADA,
                loggedUser.getLoginUsername(),
                loggedUser.getName(),
                "Paciente contemplado por " + loggedUser.getName() + " em "
                        + LocalDateTime.now().format(formatter) + " -- Motivo: " + normalizedReason,
                loggedUser);
    }

    @Transactional
    public ResultadoOperacao<Void> contemplateAppointmentByJob(
            Long appointmentId, Long medicalSlotId, Priorities contemplatedBy) {
        return contemplateAtomically(
                appointmentId,
                medicalSlotId,
                contemplatedBy,
                AppointmentStatus.PACIENTE_CONTEMPLADO,
                AUTOMATED_JOB_USER,
                AUTOMATED_JOB_USER,
                null,
                null);
    }

    private ResultadoOperacao<Void> contemplateAtomically(
            Long appointmentId,
            Long medicalSlotId,
            Priorities contemplatedBy,
            AppointmentStatus resultingStatus,
            String auditUser,
            String historyUser,
            String observation,
            SystemUserDetails loggedUser) {
        Appointment appointment = appointmentService.findForUpdateWithQueueDetails(appointmentId);
        if (appointment == null) {
            return ResultadoOperacao.falha("Marcação não encontrada.");
        }
        authorizationScopeService.requireCurrentTenant(appointment);
        if (loggedUser != null) {
            authorizationScopeService.requireBasicHealthUnit(
                    loggedUser, appointment.getPatient().getBasicHealthUnit().getId());
        }
        if (appointment.getStatus() != AppointmentStatus.AGUARDANDO_CONTEMPLACAO
                || appointment.getContemplation() != null) {
            return ResultadoOperacao.falha("A marcação já foi processada.");
        }

        MedicalSlot medicalSlot = medicalSlotService.findById(medicalSlotId);
        if (medicalSlot == null) {
            return ResultadoOperacao.falha("Vaga não encontrada.");
        }
        authorizationScopeService.requireCurrentTenant(medicalSlot);
        if (!java.util.Objects.equals(
                    medicalSlot.getBasicHealthUnit().getId(),
                    appointment.getPatient().getBasicHealthUnit().getId())
                || !java.util.Objects.equals(
                    medicalSlot.getMedicalProcedure().getId(),
                    appointment.getMedicalProcedure().getId())) {
            return ResultadoOperacao.falha("Vaga incompatível com a UBS ou procedimento da marcação.");
        }

        var slotResult = medicalSlotService.removeSlot(medicalSlot);
        if (slotResult.falhou()) {
            return ResultadoOperacao.falha(slotResult.mensagem());
        }

        LocalDateTime now = LocalDateTime.now();
        Contemplation contemplation = new Contemplation();
        contemplation.setContemplationDate(now);
        contemplation.setContemplatedBy(contemplatedBy);
        contemplation.setCreationDate(now);
        contemplation.setCreationUser(auditUser);
        contemplation.setAppointment(appointment);
        contemplation.setMedicalSlot(slotResult.valor());
        contemplation.setObservation(truncate(
                observation, ContemplationLimits.MAXIMUM_OBSERVATION_LENGTH));
        contemplation = contemplationRepository.save(contemplation);

        appointment.setContemplation(contemplation);
        appointment.setStatus(resultingStatus);
        appointment.setUpdateDate(now);
        appointment.setUpdateUser(auditUser);
        appointment = appointmentService.updateAppointment(appointment);
        appointmentStatusHistoryService.registerAppointmentStatusHistory(appointment, historyUser);
        return ResultadoOperacao.sucessoSemValor();
    }

    @Transactional
    public Contemplation registerContemplation(Contemplation contemplation) {
        return contemplationRepository.save(contemplation);
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String normalized = reason.trim();
        return normalized.length() <= ContemplationLimits.MAXIMUM_REASON_LENGTH
                ? normalized
                : null;
    }

    private ResultadoOperacao<Void> invalidContemplationReason() {
        return ResultadoOperacao.falha(
                "Motivo obrigatório e limitado a "
                        + ContemplationLimits.MAXIMUM_REASON_LENGTH
                        + " caracteres.");
    }

    private String truncate(String value, int maximumLength) {
        if (value == null || value.length() <= maximumLength) {
            return value;
        }
        return value.substring(0, maximumLength);
    }

}
