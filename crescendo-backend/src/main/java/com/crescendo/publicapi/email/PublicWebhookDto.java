package com.crescendo.publicapi.email;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.Set;

public final class PublicWebhookDto {

    private PublicWebhookDto() {}

    public record CreateWebhookRequest(
            @NotBlank String url,
            @NotEmpty Set<String> subscribedEvents
    ) {}

    /**
     * Full response returned ONCE at creation time only — includes the signing secret.
     * Do NOT return this from list/get endpoints.
     */
    public record PublicWebhookResponse(
            String id,
            String url,
            String signingSecret,
            Set<String> subscribedEvents,
            Instant createdAt
    ) {}

    /**
     * Safe list response — signing secret is intentionally omitted.
     * Secrets are shown only once at creation; clients must store them at that time.
     */
    public record PublicWebhookListResponse(
            String id,
            String url,
            Set<String> subscribedEvents,
            Instant createdAt
    ) {}
}

