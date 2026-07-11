package com.crescendo.publicapi.email;

import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_command.TemplateVariable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class PublicEmailTemplateDto {

        private PublicEmailTemplateDto() {
        }

        public record CreateTemplateRequest(
                        @NotBlank @Size(max = 255) String name,
                        @NotBlank @Size(max = 1000) String subject,
                        @NotBlank String htmlBody,
                        String textBody,
                        List<TemplateVariable> variables) {
        }

        public record UpdateTemplateRequest(
                        @Size(min = 1, max = 255) String name,
                        @Size(max = 1000) String subject,
                        String htmlBody,
                        String textBody,
                        List<TemplateVariable> variables) {
        }

        public record TestSendRequest(
                        @NotBlank String toAddress,
                        Map<String, Object> variables) {
        }

        public record PublicTemplateResponse(
                        String id,
                        String name,
                        String subject,
                        String htmlBody,
                        String textBody,
                        String status,
                        List<TemplateVariable> variables,
                        boolean hasPublishedSnapshot,
                        Instant publishedAt,
                        Instant createdAt,
                        Instant updatedAt) {
        }

        public record PublicTestSendResponse(
                        String emailLogId,
                        String status) {
        }
}
