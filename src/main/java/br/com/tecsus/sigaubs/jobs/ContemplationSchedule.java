package br.com.tecsus.sigaubs.jobs;

import br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO;
import br.com.tecsus.sigaubs.entities.Appointment;
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
public class ContemplationSchedule {

    private final MedicalSlotService medicalSlotService;
    private final AppointmentService appointmentService;
    private final ContemplationService contemplationService;
    private final AppointmentStatusHistoryService appointmentStatusHistoryService;
    private static final int MAX_ATTEMPTS = 4;
    private final String USERNAME_JOB = "ROTINA";

    @Autowired
    public ContemplationSchedule(MedicalSlotService medicalSlotService, AppointmentService appointmentService,
            ContemplationService contemplationService,
            AppointmentStatusHistoryService appointmentStatusHistoryService) {
        this.medicalSlotService = medicalSlotService;
        this.appointmentService = appointmentService;
        this.contemplationService = contemplationService;
        this.appointmentStatusHistoryService = appointmentStatusHistoryService;
    }

    @Transactional
    @Scheduled(cron = "${schedule.cron.contemplation}")
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
        final int NEXT_CONTEMPLATED = 1;

        YearMonth referenceMonth = YearMonth.now();
        List<MedicalSlot> availableSlots = medicalSlotService.findAvailableSlotsByReferenceMonth();

        log.info("==> Carregando vagas disponíveis");
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

        log.info(" ");
        log.info("======== INICIANDO CONTEMPLAÇÕES POR UBS ========");
        log.info(" ");
        for (Map.Entry<BasicHealthUnit, List<MedicalSlot>> ubsSlots : slotsByUBS.entrySet()) {

            log.info(":::::::::::::::::::::::::::: [{}] ::::::::::::::::::::::::::::", ubsSlots.getKey().getName());
            for (MedicalSlot slotsByProcedure : ubsSlots.getValue()) {
                log.info(" ");
                log.info(">>> Vagas disponíveis para {}[{}][{}]: {}",
                        slotsByProcedure.getMedicalProcedure().getDescription(),
                        slotsByProcedure.getMedicalProcedure().getProcedureType().name(),
                        slotsByProcedure.getMedicalProcedure().getSpecialty().getDescription(),
                        slotsByProcedure.getCurrentSlots());

                log.info(">> Carregando fila de espera...");
                Page<PatientOpenAppointmentDTO> queue = appointmentService
                        .findOpenAppointmentsQueuePaginatedV2(
                                ubsSlots.getKey().getId(),
                                null,
                                slotsByProcedure.getMedicalProcedure().getId(),
                                PageRequest.of(0, slotsByProcedure.getCurrentSlots() + NEXT_CONTEMPLATED));
                log.info(">> [{}] pacientes carregados da fila de espera.", queue.getContent().size());
                log.info(">> Iniciando contemplação...");
                log.info("::::::::: [NOME DO PACIENTE] ::::::::: [CPF] ::::::::: [CONTEMPLADO POR] :::::::::");
                for (int slot = 0; slot < slotsByProcedure.getCurrentSlots(); slot++) {

                    Appointment appt = appointmentService
                            .findReferenceById(queue.getContent().get(slot).appointmentId());
                    appt.setStatus(AppointmentStatus.PACIENTE_CONTEMPLADO);

                    Contemplation contemplated = new Contemplation();

                    // appt.setId(queue.getContent().get(slot).appointmentId());
                    contemplated.setMedicalSlot(slotsByProcedure);
                    contemplated.setAppointment(appt);
                    contemplated.setContemplatedBy(contemplatedBy(queue.getContent().get(slot),
                            queue.getContent().get(slot + NEXT_CONTEMPLATED)));
                    contemplated.setContemplationDate(LocalDateTime.now());
                    contemplated.setCreationDate(LocalDateTime.now());
                    contemplated.setCreationUser("Rotina de Contemplação");

                    appt = appointmentService.updateAppointment(appt);
                    contemplationService.registerContemplation(contemplated);
                    appointmentStatusHistoryService.registerAppointmentStatusHistory(
                            appt,
                            USERNAME_JOB);
                    medicalSlotService.removeSlot(slotsByProcedure);

                    log.info("> Contemplado: [{}] [{}] [{}]",
                            queue.getContent().get(slot).patientName(),
                            queue.getContent().get(slot).patientCPF(),
                            contemplated.getContemplatedBy().getDescription());
                }

                log.info("[X]------[X]------[X]------[X]------[X]------[X]------[X]");

            }
            log.info("::::::::::::::::::: FIM DA CONTEMPLAÇÃO[{}] :::::::::::::::::::", ubsSlots.getKey().getName());
        }

        ContemplationScheduleStatus.status = ContemplationScheduleStatus.Status.DONE;
        ContemplationScheduleStatus.endTime = LocalDateTime.now();

        log.info(" ");
        log.info("========================================");
        log.info("=== ROTINA DE CONTEMPLAÇÃO FINALIZADA ===");
        log.info("========================================");
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

    private Priorities contemplatedBy(PatientOpenAppointmentDTO currentContemplated,
            PatientOpenAppointmentDTO nextContemplated) {

        if (currentContemplated.requestDate().isBefore(LocalDateTime.now().minusMonths(QUATRO_MESES))) {
            return Priorities.MAIS_DE_QUATRO_MESES;
        } else if (currentContemplated.priority().getValue() < nextContemplated.priority().getValue()) {
            return currentContemplated.priority();
        } else if (currentContemplated.patientBirthDate().isBefore(nextContemplated.patientBirthDate())) {
            return Priorities.IDADE;
        } else if (currentContemplated.patientSocialSituationRating().getPriority() > nextContemplated
                .patientSocialSituationRating().getPriority()) {
            return Priorities.SITUACAO_SOCIAL;
        } else if (currentContemplated.patientGender().equals("Feminino")) {
            return Priorities.SEXO;
        } else {
            return Priorities.DATA_DA_MARCACAO;
        }
    }

}
