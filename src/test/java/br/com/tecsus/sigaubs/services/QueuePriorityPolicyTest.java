package br.com.tecsus.sigaubs.services;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class QueuePriorityPolicyTest {

    @Test
    void deveAplicarLimiteDeQuatroMesesSemAlterarARegraDeFronteira() {
        LocalDateTime referenceDate = LocalDateTime.of(2026, 7, 29, 12, 0);
        LocalDateTime cutoff = LocalDateTime.of(2026, 3, 29, 12, 0);

        assertThat(QueuePriorityPolicy.longWaitingCutoff(referenceDate))
                .isEqualTo(cutoff);
        assertThat(QueuePriorityPolicy.hasLongWaitingTime(
                cutoff.minusNanos(1), referenceDate)).isTrue();
        assertThat(QueuePriorityPolicy.hasLongWaitingTime(
                cutoff, referenceDate)).isFalse();
    }

    @Test
    void deveAplicarPrioridadeDeGeneroComValoresDoDominio() {
        assertThat(QueuePriorityPolicy.hasGenderPriority(
                "Feminino", "Masculino")).isTrue();
        assertThat(QueuePriorityPolicy.hasGenderPriority(
                "Masculino", "Feminino")).isFalse();
    }
}
