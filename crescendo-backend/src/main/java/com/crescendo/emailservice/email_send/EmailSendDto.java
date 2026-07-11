package com.crescendo.emailservice.email_send;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;
import java.util.UUID;

public final class EmailSendDto {

    private EmailSendDto() {}

    /// Request body for the public POST /api/v1/emails endpoint.
    /// Mirrors Resend's API design for familiarity.
    public record SendEmailRequest(
            @NotBlank @Email String from,
            @NotBlank @Email String to,
            @NotBlank String subject,
            String htmlBody,
            String textBody,
            UUID templateId,
            Map<String, Object> templateData,
            @jakarta.validation.constraints.NotNull com.crescendo.enums.EmailType emailType
    ) {}

    public record SendEmailResponse(
            UUID id,
            String to,
            String from,
            String subject,
            String status
    ) {}

    public record SendTemplatedRequest(
            @NotBlank @Email String from,
            @NotBlank @Email String to,
            @jakarta.validation.constraints.NotNull UUID templateId,
            Map<String, Object> templateData,
            com.crescendo.enums.EmailType emailType
    ) {}

    public record SendBatchRequest(
            @NotBlank @Email String from,
            @NotBlank String subject,
            String htmlBody,
            String textBody,
            UUID templateId,
            Map<String, Object> templateData,
            com.crescendo.enums.EmailType emailType,
            @jakarta.validation.constraints.NotEmpty java.util.List<@Email String> to
    ) {}

    public record SendBatchResponse(
            java.util.List<SendEmailResponse> emails
    ) {}
}
