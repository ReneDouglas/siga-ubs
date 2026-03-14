package br.com.tecsus.sigaubs.jobs;

import br.com.tecsus.sigaubs.services.ContemplationScheduleService;
import br.com.tecsus.sigaubs.utils.ContemplationScheduleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ContemplationScheduleV2 {

    private static final Logger log = LoggerFactory.getLogger(ContemplationScheduleV2.class);

    private final ContemplationScheduleService contemplationScheduleService;
    private final ContemplationScheduleStatus contemplationScheduleStatus;

    private static final int MAX_ATTEMPTS = 4;

    @Autowired
    public ContemplationScheduleV2(ContemplationScheduleService contemplationScheduleService,
            ContemplationScheduleStatus contemplationScheduleStatus) {
        this.contemplationScheduleService = contemplationScheduleService;
        this.contemplationScheduleStatus = contemplationScheduleStatus;
    }

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

        contemplationScheduleStatus.setRunning();
        contemplationScheduleService.executeContemplation();
        contemplationScheduleStatus.setDone();
    }

    @Recover
    private void failedContemplationScheduleTask(RuntimeException e) {

        log.error("[retry] Todas as tentativas da rotina de contemplação falharam.");
        log.error("[retry] Número de tentativas: {}", RetrySynchronizationManager.getContext().getRetryCount());
        log.error("[retry] Erro: {}", e.getMessage());
        log.error("[retry] Finalizando rotina de contemplação.");

        contemplationScheduleStatus.setFailed();
    }
}
