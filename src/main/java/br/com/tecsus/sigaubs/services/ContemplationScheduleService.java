package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.dtos.PatientOpenAppointmentDTO;
import br.com.tecsus.sigaubs.entities.BasicHealthUnit;
import br.com.tecsus.sigaubs.entities.Contemplation;
import br.com.tecsus.sigaubs.entities.MedicalSlot;
import br.com.tecsus.sigaubs.enums.AppointmentStatus;
import br.com.tecsus.sigaubs.enums.Priorities;
import br.com.tecsus.sigaubs.tenancy.TenantContextHolder;
import br.com.tecsus.sigaubs.tenancy.TenantResolverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static br.com.tecsus.sigaubs.utils.DefaultValues.QUATRO_MESES;

@Service
public class ContemplationScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ContemplationScheduleService.class);

    private static final int NEXT_PATIENT = 1;
    private static final String USERNAME_JOB = "ROTINA";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Runnable NO_OP_HEARTBEAT = () -> {
    };

    private final MedicalSlotService medicalSlotService;
    private final AppointmentService appointmentService;
    private final ContemplationService contemplationService;
    private final AppointmentStatusHistoryService appointmentStatusHistoryService;
    private final TenantResolverService tenantResolverService;
    private final TransactionTemplate transactionTemplate;
    private final ContemplationJobExecutionService executionService;

    @Autowired
    public ContemplationScheduleService(MedicalSlotService medicalSlotService,
            AppointmentService appointmentService,
            ContemplationService contemplationService,
            AppointmentStatusHistoryService appointmentStatusHistoryService,
            TenantResolverService tenantResolverService,
            TransactionTemplate transactionTemplate,
            ContemplationJobExecutionService executionService) {
        this.medicalSlotService = medicalSlotService;
        this.appointmentService = appointmentService;
        this.contemplationService = contemplationService;
        this.appointmentStatusHistoryService = appointmentStatusHistoryService;
        this.tenantResolverService = tenantResolverService;
        this.transactionTemplate = transactionTemplate;
        this.executionService = executionService;
    }

    ContemplationScheduleService(MedicalSlotService medicalSlotService,
            AppointmentService appointmentService,
            ContemplationService contemplationService,
            AppointmentStatusHistoryService appointmentStatusHistoryService,
            TenantResolverService tenantResolverService,
            TransactionTemplate transactionTemplate) {
        this(medicalSlotService, appointmentService, contemplationService,
                appointmentStatusHistoryService, tenantResolverService, transactionTemplate, null);
    }

    public void executeContemplation() {
        var tenants = tenantResolverService.findActiveTenants();
        LocalDate executionDate = LocalDate.now(BUSINESS_ZONE);
        LocalDateTime windowStart = executionDate.atStartOfDay();
        String executionKey = "contemplation:" + executionDate;

        if (tenants.isEmpty()) {
            log.info("Nenhum tenant ativo encontrado para executar a rotina de contemplação.");
            return;
        }

        RuntimeException firstFailure = null;

        for (var tenant : tenants) {
            ContemplationJobExecutionService.Lease lease = null;
            if (executionService != null) {
                var acquiredLease =
                        executionService.tryStart(tenant.getId(), executionKey, windowStart);
                if (acquiredLease.isEmpty()) {
                    log.info("Rotina já adquirida ou concluída para tenant_id={}.", tenant.getId());
                    continue;
                }
                lease = acquiredLease.get();
            }
            ContemplationJobExecutionService.Lease currentLease = lease;
            Runnable heartbeat = heartbeatFor(currentLease);
            TenantContextHolder.setTenant(tenant.getId(), tenant.getSlug());
            try {
                transactionTemplate.executeWithoutResult(
                        status -> executeContemplationForCurrentTenant(heartbeat));
                heartbeat.run();
                if (currentLease != null) {
                    executionService.complete(currentLease);
                }
            } catch (RuntimeException e) {
                if (currentLease != null) {
                    executionService.fail(currentLease);
                }
                log.error("Erro na rotina de contemplação para tenant_id={} [{}].",
                        tenant.getId(), e.getClass().getSimpleName());
                if (firstFailure == null) {
                    firstFailure = e;
                }
            } finally {
                TenantContextHolder.clear();
            }
        }

        if (firstFailure != null) {
            throw firstFailure;
        }
    }

    private Runnable heartbeatFor(ContemplationJobExecutionService.Lease lease) {
        if (executionService == null || lease == null) {
            return NO_OP_HEARTBEAT;
        }
        var renewalSchedule = executionService.newRenewalSchedule();
        return () -> {
            if (renewalSchedule.claimIfDue()) {
                executionService.renewLease(lease);
            }
        };
    }

    private void executeContemplationForCurrentTenant(Runnable heartbeat) {
        heartbeat.run();
        YearMonth referenceMonth = YearMonth.now();
        var availableSlots = medicalSlotService.findAvailableSlotsByReferenceMonth();

        log.info("==> Carregando todas as vagas disponíveis");
        log.info("> Tenant: {}", TenantContextHolder.getRequiredTenantSlug());
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

        processSlotsByUBS(slotsByUBS, heartbeat);
    }

    private void processSlotsByUBS(
            Map<BasicHealthUnit, List<MedicalSlot>> slotsByUBS,
            Runnable heartbeat) {

        log.info(" ");
        log.info("======== INICIANDO CONTEMPLAÇÕES POR UBS ========");
        log.info(" ");

        slotsByUBS.forEach((ubs, slots) -> {
            heartbeat.run();
            log.info("::::::::::::::::::INICIO DA CONTEMPLAÇÃO [{}] ::::::::::::::::::", ubs.getName());
            slots.forEach(slot -> processSlotsByProcedure(slot, heartbeat));
            log.info("::::::::::::::::::: FIM DA CONTEMPLAÇÃO [{}] :::::::::::::::::::", ubs.getName());
        });

        log.info(" ");
        log.info("========================================");
        log.info("=== ROTINA DE CONTEMPLAÇÃO FINALIZADA ===");
        log.info("========================================");
    }

    private void processSlotsByProcedure(
            MedicalSlot slotsByProcedure,
            Runnable heartbeat) {

        heartbeat.run();
        log.info(" ");
        log.info(">>> Vagas disponíveis para {}[{}][{}]: {}",
                slotsByProcedure.getMedicalProcedure().getDescription(),
                slotsByProcedure.getMedicalProcedure().getProcedureType().name(),
                slotsByProcedure.getMedicalProcedure().getSpecialty().getTitle(),
                slotsByProcedure.getCurrentSlots());

        var queue = loadAppointmentQueue(slotsByProcedure);
        log.info(">> [{}] pacientes carregados da fila de espera.", queue.getContent().size());

        if (queue.getContent().isEmpty()) {
            log.info(">> Nenhum paciente na fila para este procedimento. Pulando.");
            return;
        }

        log.info(">> Iniciando contemplação...");
        log.info("::::::::: [ID DA MARCAÇÃO] ::::::::: [CRITÉRIO] :::::::::");

        int totalPatients = queue.getContent().size() - 1;

        for (int slot = 0; slot < slotsByProcedure.getCurrentSlots(); slot++) {
            heartbeat.run();

            if (slot > totalPatients) {
                log.info(">> Vagas restantes sem pacientes na fila. Encerrando.");
                break;
            }

            var currentPatient = queue.getContent().get(slot);
            var nextPatient = queue.getContent().get(totalPatients > slot ? slot + NEXT_PATIENT : slot);

            contemplatePatient(currentPatient, nextPatient, slotsByProcedure);
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

    private void contemplatePatient(PatientOpenAppointmentDTO currentPatient,
            PatientOpenAppointmentDTO nextPatient,
            MedicalSlot slotsByProcedure) {

        Priorities criterion = contemplatedBy(currentPatient, nextPatient);
        var result = contemplationService.contemplateAppointmentByJob(
                currentPatient.appointmentId(), slotsByProcedure.getId(), criterion);
        if (result.falhou()) {
            log.warn("> Marcação id={} não contemplada: {}",
                    currentPatient.appointmentId(), result.mensagem());
            return;
        }
        log.info("> Marcação id={} contemplada pelo critério={}.",
                currentPatient.appointmentId(), criterion.name());
    }

    private Priorities contemplatedBy(PatientOpenAppointmentDTO currentPatient,
            PatientOpenAppointmentDTO nextPatient) {

        if (currentPatient.requestDate().isBefore(LocalDateTime.now().minusMonths(QUATRO_MESES))) {
            return Priorities.MAIS_DE_QUATRO_MESES;
        } else if (currentPatient.priority().getValue() < nextPatient.priority().getValue()) {
            return currentPatient.priority();
        } else if (currentPatient.patientBirthDate().isBefore(nextPatient.patientBirthDate())) {
            return Priorities.IDADE;
        } else if (currentPatient.patientSocialSituationRating().getPriority() < nextPatient
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
