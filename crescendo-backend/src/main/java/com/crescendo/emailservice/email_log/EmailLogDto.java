package com.crescendo.emailservice.email_log;

import java.time.Instant;
import java.util.UUID;

/**
 * DTOs for email log / tracking endpoints.
 */
public final class EmailLogDto {

    private EmailLogDto() {}

    /**
     * Response returned when querying a single email or listing emails.
     */
    public record EmailLogResponse(
            UUID id,
            String to,
            String from,
            String subject,
            String status,
            String provider,
            String providerMessageId,
            String error,
            UUID templateId,
            Instant createdAt,
            Instant sentAt,
            Instant openedAt,
            int openCount,
            int clickCount
    ) {}
}
