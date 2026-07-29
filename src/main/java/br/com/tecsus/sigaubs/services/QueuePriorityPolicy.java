package br.com.tecsus.sigaubs.services;

import br.com.tecsus.sigaubs.enums.PatientGender;

import java.time.LocalDateTime;
import java.time.Period;

public final class QueuePriorityPolicy {

    private static final Period LONG_WAITING_PERIOD = Period.ofMonths(4);

    private QueuePriorityPolicy() {
    }

    public static LocalDateTime longWaitingCutoff(LocalDateTime referenceDate) {
        return referenceDate.minus(LONG_WAITING_PERIOD);
    }

    public static boolean hasLongWaitingTime(
            LocalDateTime requestDate,
            LocalDateTime referenceDate) {
        return requestDate.isBefore(longWaitingCutoff(referenceDate));
    }

    public static boolean hasGenderPriority(String currentGender, String nextGender) {
        return PatientGender.FEMININO.matches(currentGender)
                && PatientGender.MASCULINO.matches(nextGender);
    }
}
