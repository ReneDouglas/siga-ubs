package br.com.tecsus.sigaubs.jobs;

import br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO;
import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.Contemplation;
import br.com.tecsus.sigaubs.entities.MedicalSlot;
import br.com.tecsus.sigaubs.enums.AppointmentStatus;
import br.com.tecsus.sigaubs.enums.Priorities;
import br.com.tecsus.sigaubs.services.AppointmentService;
import br.com.tecsus.sigaubs.services.AppointmentStatusHistoryService;
import br.com.tecsus.sigaubs.services.ContemplationService;
import br.com.tecsus.sigaubs.services.MedicalSlotService;
import br.com.tecsus.sigaubs.utils.ContemplationScheduleStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static br.com.tecsus.sigaubs.utils.DefaultValues.QUATRO_MESES;

@Slf4j
@Component
public class ContemplationScheduleV2 {

    private final MedicalSlotService medicalSlotService;
    private final AppointmentService appointmentService;
    private final ContemplationService contemplationService;
    private final AppointmentStatusHistoryService appointmentStatusHistoryService;

    private static final int NEXT_PATIENT = 1;
    private static final int MAX_ATTEMPTS = 4;
    private static final String USERNAME_JOB = "ROTINA";

    @Autowired
    public ContemplationScheduleV2(MedicalSlotService medicalSlotService, AppointmentService appointmentService,
            ContemplationService contemplationService,
            AppointmentStatusHistoryService appointmentStatusHistoryService) {
        this.medicalSlotService = medicalSlotService;
        this.appointmentService = appointmentService;
        this.contemplationService = contemplationService;
        this.appointmentStatusHistoryService = appointmentStatusHistoryService;
    }

    @Transactional
    @Scheduled(cron = "${schedule.cron.contemplation}", zone = "${schedule.cron.contemplation.zone}")
    @Retryable(retryFor = RuntimeException.class, maxAttempts = MAX_ATTEMPTS, backoff = @Backoff(delay = 5000))
    public void processContemplationTask() throws RuntimeException {

        if (RetrySynchronizationManager.getContext().getRetryCount() > 0) {
            log.warn("[retry] A rotina de contemplação falhou.");
            log.warn("[retry] Mensagem de erro: {}",
                    RetrySynchronizationManager.getContext().getLastThrowable().getMessage());
            log.warn("[retry] Tentativa {} de {}", RetrySynchronizationManager.getContext().getRetryCount(),
                    MAX_ATTEMPTS);
        }

        log.info(" ");
        log.info("========================================");
        log.info("=== INICIANDO ROTINA DE CONTEMPLAÇÃO ===");
        log.info("========================================");
        log.info(" ");

        ContemplationScheduleStatus.status = ContemplationScheduleStatus.Status.RUNNING;
        ContemplationScheduleStatus.startTime = LocalDateTime.now();

        YearMonth referenceMonth = YearMonth.now();
        var availableSlots = medicalSlotService.findAvailableSlotsByReferenceMonth();

        log.info("==> Carregando todas as vagas disponíveis");
        log.info("> Mês de Referência: {}", referenceMonth.getMonth().name().toUpperCase());

        if (availableSlots.isEmpty()) {
            log.info("> Total de Vagas: 0");
            log.info("========================================");
            log.info("=== ROTINA DE CONTEMPLAÇÃO FINALIZADA ===");
            log.info("========================================");
            return;
        }

        log.info("> Total de Vagas: {}", availableSlots.stream().mapToInt(MedicalSlot::getCurrentSlots).sum());

        Map<BasicHealthUnit, List<MedicalSlot>> slotsByUBS = availableSlots.stream()
                .collect(Collectors.groupingBy(MedicalSlot::getBasicHealthUnit));

        processSlotsByUBS(slotsByUBS);

        ContemplationScheduleStatus.status = ContemplationScheduleStatus.Status.DONE;
        ContemplationScheduleStatus.endTime = LocalDateTime.now();
    }

    private void processSlotsByUBS(Map<BasicHealthUnit, List<MedicalSlot>> slotsByUBS) {

        log.info(" ");
        log.info("======== INICIANDO CONTEMPLAÇÕES POR UBS ========");
        log.info(" ");

        slotsByUBS.forEach((ubs, slots) -> {
            log.info("::::::::::::::::::INICIO DA CONTEMPLAÇÃO [{}] ::::::::::::::::::", ubs.getName());
            slots.forEach(this::processSlotsByProcedure);
            log.info("::::::::::::::::::: FIM DA CONTEMPLAÇÃO [{}] :::::::::::::::::::", ubs.getName());
        });

        log.info(" ");
        log.info("========================================");
        log.info("=== ROTINA DE CONTEMPLAÇÃO FINALIZADA ===");
        log.info("========================================");
    }

    private void processSlotsByProcedure(MedicalSlot slotsByProcedure) {

        log.info(" ");
        log.info(">>> Vagas disponíveis para {}[{}][{}]: {}",
                slotsByProcedure.getMedicalProcedure().getDescription(),
                slotsByProcedure.getMedicalProcedure().getProcedureType().name(),
                slotsByProcedure.getMedicalProcedure().getSpecialty().getTitle(),
                slotsByProcedure.getCurrentSlots());

        var queue = loadAppointmentQueue(slotsByProcedure);
        log.info(">> [{}] pacientes carregados da fila de espera.", queue.getContent().size());
        log.info(">> Iniciando contemplação...");
        log.info("::::::::: [NOME DO PACIENTE] ::::::::: [CPF] ::::::::: [CONTEMPLADO POR] :::::::::");

        int totalPatients = queue.getContent().size() - 1;

        for (int slot = 0; slot < slotsByProcedure.getCurrentSlots(); slot++) {

            var currentPatient = queue.getContent().get(slot);
            var nextPatient = queue.getContent().get(totalPatients > slot ? slot + NEXT_PATIENT : slot);

            contemplatePatient(
                    currentPatient,
                    nextPatient,
                    slotsByProcedure);
        }

        log.info("[X]------[X]------[X]------[X]------[X]------[X]------[X]");
    }

    private Page<PatientOpenAppointmentDTO> loadAppointmentQueue(MedicalSlot slotsByProcedure) {
        return appointmentService.findOpenAppointmentsQueuePaginatedV2(
                slotsByProcedure.getBasicHealthUnit().getId(),
                null,
                slotsByProcedure.getMedicalProcedure().getId(),
                PageRequest.of(0, slotsByProcedure.getCurrentSlots() + NEXT_PATIENT));
    }

    private void contemplatePatient(PatientOpenAppointmentDTO currentPatient, PatientOpenAppointmentDTO nextPatient,
            MedicalSlot slotsByProcedure) {

        var contemplated = new Contemplation();
        contemplated.setMedicalSlot(slotsByProcedure);
        contemplated.setContemplatedBy(contemplatedBy(currentPatient, nextPatient));
        contemplated.setContemplationDate(LocalDateTime.now());
        contemplated.setCreationDate(LocalDateTime.now());
        contemplated.setCreationUser(USERNAME_JOB);

        contemplated = contemplationService.registerContemplation(contemplated);
        medicalSlotService.removeSlot(slotsByProcedure);

        var appt = appointmentService.findReferenceById(currentPatient.appointmentId());
        appt.setContemplation(contemplated);
        appt.setStatus(AppointmentStatus.PACIENTE_CONTEMPLADO);
        appt = appointmentService.updateAppointment(appt);

        appointmentStatusHistoryService.registerAppointmentStatusHistory(appt, USERNAME_JOB);

        log.info("> Contemplado: [{}] [{}] [{}]",
                currentPatient.patientName(),
                currentPatient.patientCPF(),
                contemplated.getContemplatedBy().getDescription());
    }

    @Recover
    private void failedContemplationScheduleTask(RuntimeException e) {

        log.error("[retry] Todas as tentativas da rotina de contemplação falharam.");
        log.error("[retry] Número de tentativas: {}", RetrySynchronizationManager.getContext().getRetryCount());
        log.error("[retry] Erro: {}", e.getMessage());
        log.error("[retry] Finalizando rotina de contemplação.");

        ContemplationScheduleStatus.status = ContemplationScheduleStatus.Status.FAILED;
        ContemplationScheduleStatus.endTime = LocalDateTime.now();
    }

    private Priorities contemplatedBy(PatientOpenAppointmentDTO currentPatient, PatientOpenAppointmentDTO nextPatient) {

        if (currentPatient.requestDate().isBefore(LocalDateTime.now().minusMonths(QUATRO_MESES))) {
            return Priorities.MAIS_DE_QUATRO_MESES;
        } else if (currentPatient.priority().getValue() < nextPatient.priority().getValue()) {
            return currentPatient.priority();
        } else if (currentPatient.patientBirthDate().isBefore(nextPatient.patientBirthDate())) {
            return Priorities.IDADE;
        } else if (currentPatient.patientSocialSituationRating().getPriority() > nextPatient
                .patientSocialSituationRating().getPriority()) {
            return Priorities.SITUACAO_SOCIAL;
        } else if (currentPatient.patientGender().equals("Feminino")
                && nextPatient.patientGender().equals("Masculino")) {
            return Priorities.SEXO;
        } else {
            return Priorities.DATA_DA_MARCACAO;
        }
    }

}
