package br.com.tecsus.sigaubs.dtos;

import java.time.Duration;
import java.time.Instant;

public record SessionSummaryDTO(
        String managementId,
        Instant createdAt,
        Instant lastAccessedAt,
        Duration maximumIdleTime,
        String clientDescription,
        String locationDescription,
        boolean current,
        String ownerDescription) {

    public SessionSummaryDTO(
            String managementId,
            Instant createdAt,
            Instant lastAccessedAt,
            Duration maximumIdleTime,
            String clientDescription,
            boolean current) {
        this(
                managementId,
                createdAt,
                lastAccessedAt,
                maximumIdleTime,
                clientDescription,
                "Localidade indisponível",
                current,
                null);
    }
}
