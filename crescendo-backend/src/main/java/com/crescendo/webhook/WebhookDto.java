package com.crescendo.webhook;

import java.time.Instant;
import java.util.UUID;

/**
 * DTOs for webhook management endpoints.
 */
public class WebhookDto {

    /**
     * Read representation of a webhook — excludes internal details.
     */
    public record WebhookResponse(
            UUID id,
            String webhookKey,
            UUID stepId,
            boolean isActive,
            String webhookUrl,
            Instant createdAt
    ) {}
}
