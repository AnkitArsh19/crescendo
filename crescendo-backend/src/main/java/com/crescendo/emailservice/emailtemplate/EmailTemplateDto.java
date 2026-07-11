package com.crescendo.emailservice.emailtemplate;

import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_command.TemplateVariable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EmailTemplateDto {

    private EmailTemplateDto() {}

    // ── Requests ─────────────────────────────────────────────────────────────

    public record CreateTemplateRequest(
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Size(max = 1000) String subject,
            @NotBlank String htmlBody,
            String textBody,
            List<TemplateVariable> variables  // optional at creation — can be added later
    ) {}

    public record UpdateTemplateRequest(
            @Size(min = 1, max = 255) String name,
            @Size(max = 1000) String subject,
            String htmlBody,
            String textBody,
            List<TemplateVariable> variables
    ) {}

    /**
     * Test-send a draft or published template with example variable values.
     * Sends to the caller's own verified email address; tagged isTest=true in logs.
     */
    public record TestSendRequest(
            @NotBlank String toAddress,   // target address (must be caller's verified address or any in dev)
            Map<String, Object> variables // example values to substitute into the template
    ) {}

    // ── Responses ─────────────────────────────────────────────────────────────

    public record TemplateResponse(
            UUID id,
            String name,
            String subject,
            String htmlBody,
            String textBody,
            String status,                       // DRAFT | PUBLISHED
            List<TemplateVariable> variables,
            boolean hasPublishedSnapshot,        // true if template has been published at least once
            Instant publishedAt,                 // null if never published
            Instant createdAt,
            Instant updatedAt
    ) {}

    /** Returned from POST /settings/templates/{id}/test-send */
    public record TestSendResponse(
            String emailId,
            String to,
            String subject,
            boolean isTest
    ) {}
}

