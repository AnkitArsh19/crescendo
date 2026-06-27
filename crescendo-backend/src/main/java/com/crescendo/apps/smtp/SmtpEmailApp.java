package com.crescendo.apps.smtp;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AppDefinition for SMTP Email.
 */
@Component
public class SmtpEmailApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App(
                "smtp",
                "SMTP Email",
                """
                SMTP integration allows you to send emails using your own email server.
                
                This integration provides operations for:
                - **Send Email**: Send an email
                - **Send and Wait**: Send an email and wait for a reply
                
                Authenticate using SMTP credentials (username, password, host, port).
                """,
                "/icons/mail.svg", // Generic icon
                AuthType.APIKEY, // Uses custom credentials map
                List.of(),
                List.of(
                        Map.of(
                                "actionKey", "smtp:send",
                                "name", "Send Email",
                                "description", "Send an email",
                                "configSchema", List.of(
                                        Map.of("key", "fromEmail", "label", "From Email", "type", "text", "required", true),
                                        Map.of("key", "toEmail", "label", "To Email", "type", "text", "required", true),
                                        Map.of("key", "ccEmail", "label", "CC Email", "type", "text"),
                                        Map.of("key", "bccEmail", "label", "BCC Email", "type", "text"),
                                        Map.of("key", "subject", "label", "Subject", "type", "text", "required", true),
                                        Map.of("key", "text", "label", "Text", "type", "text"),
                                        Map.of("key", "html", "label", "HTML", "type", "text"),
                                        Map.of("key", "attachments", "label", "Attachments (JSON array)", "type", "json")
                                )
                        ),
                        Map.of(
                                "actionKey", "smtp:sendAndWait",
                                "name", "Send and Wait",
                                "description", "Send an email and wait for a reply",
                                "configSchema", List.of(
                                        Map.of("key", "fromEmail", "label", "From Email", "type", "text", "required", true),
                                        Map.of("key", "toEmail", "label", "To Email", "type", "text", "required", true),
                                        Map.of("key", "ccEmail", "label", "CC Email", "type", "text"),
                                        Map.of("key", "bccEmail", "label", "BCC Email", "type", "text"),
                                        Map.of("key", "subject", "label", "Subject", "type", "text", "required", true),
                                        Map.of("key", "text", "label", "Text", "type", "text"),
                                        Map.of("key", "html", "label", "HTML", "type", "text"),
                                        Map.of("key", "attachments", "label", "Attachments (JSON array)", "type", "json")
                                )
                        )
                )
        ).credentialSchema(List.of(
                Map.of("key", "user", "label", "User", "type", "text", "required", true),
                Map.of("key", "password", "label", "Password", "type", "password", "required", true),
                Map.of("key", "host", "label", "Host", "type", "text", "required", true),
                Map.of("key", "port", "label", "Port", "type", "number", "required", true, "default", 465),
                Map.of("key", "secure", "label", "SSL/TLS", "type", "boolean", "default", true),
                Map.of("key", "disableStartTls", "label", "Disable STARTTLS", "type", "boolean", "default", false),
                Map.of("key", "hostName", "label", "Client Host Name", "type", "text")
        )).category("messaging");
    }
}
