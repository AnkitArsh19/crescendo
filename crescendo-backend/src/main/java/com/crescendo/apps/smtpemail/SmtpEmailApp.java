package com.crescendo.apps.smtpemail;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SmtpEmailApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("smtp-email", "SMTP Email", "Send email using a user-owned SMTP account",
                "/icons/crescendo-email.svg", AuthType.APIKEY,
                List.of(),
                List.of(Map.of(
                        "actionKey", "send-email",
                        "name", "Send Email",
                        "description", "Send an email with optional CC, BCC, HTML/text body, and Base64 attachments",
                        "configSchema", List.of(
                                Map.of("key", "to", "label", "To", "type", "text", "required", true, "placeholder", "user@example.com"),
                                Map.of("key", "from", "label", "From", "type", "text", "required", false, "placeholder", "sender@example.com"),
                                Map.of("key", "cc", "label", "CC", "type", "text", "required", false),
                                Map.of("key", "bcc", "label", "BCC", "type", "text", "required", false),
                                Map.of("key", "subject", "label", "Subject", "type", "text", "required", true),
                                Map.of("key", "htmlBody", "label", "HTML Body", "type", "textarea", "required", false),
                                Map.of("key", "textBody", "label", "Text Body", "type", "textarea", "required", false),
                                Map.of("key", "attachmentsJson", "label", "Attachments JSON", "type", "json", "required", false,
                                        "placeholder", "[{\"filename\":\"report.pdf\",\"contentType\":\"application/pdf\",\"base64\":\"...\"}]")
                        )
                )))
                .credentialSchema(List.of(
                        Map.of("key", "host", "label", "SMTP Host", "type", "text", "required", true, "placeholder", "smtp.gmail.com"),
                        Map.of("key", "port", "label", "SMTP Port", "type", "text", "required", false, "placeholder", "587"),
                        Map.of("key", "username", "label", "Username", "type", "text", "required", false),
                        Map.of("key", "password", "label", "Password", "type", "password", "required", false),
                        Map.of("key", "defaultFrom", "label", "Default From", "type", "text", "required", false),
                        Map.of("key", "startTls", "label", "STARTTLS", "type", "boolean", "required", false),
                        Map.of("key", "ssl", "label", "SSL", "type", "boolean", "required", false),
                        Map.of("key", "auth", "label", "SMTP Auth", "type", "boolean", "required", false)))
                .category("communication")
                .helpUrl("https://jakarta.ee/specifications/mail/");
    }
}
