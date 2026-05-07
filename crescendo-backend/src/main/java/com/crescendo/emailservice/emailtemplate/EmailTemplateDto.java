package com.crescendo.emailservice.emailtemplate;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class EmailTemplateDto {

    private EmailTemplateDto() {}

    public record CreateTemplateRequest(
            @NotBlank String name,
            @NotBlank String subject,
            @NotBlank String htmlBody,
            String textBody
    ) {}

    public record UpdateTemplateRequest(
            String name,
            String subject,
            String htmlBody,
            String textBody
    ) {}

    public record TemplateResponse(
            UUID id,
            String name,
            String subject,
            String htmlBody,
            String textBody,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
