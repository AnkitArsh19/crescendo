package com.crescendo.apps.email;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class CrescendoEmailApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("crescendo-email", "Crescendo Email",
                "Send transactional emails using the built-in email service — no external credentials needed",
                "/icons/crescendo-email.svg", AuthType.NONE,
                List.of(),
                List.of(
                    Map.of("actionKey", "send-email", "name", "Send Email",
                        "description", "Send an email via Crescendo's built-in email service",
                        "configSchema", List.of(
                            Map.of("key", "to", "label", "To", "type", "text", "required", true,
                                   "placeholder", "user@example.com", "helpText", "Recipient email address"),
                            Map.of("key", "subject", "label", "Subject", "type", "text", "required", true,
                                   "placeholder", "Your workflow completed", "helpText", "Email subject line"),
                            Map.of("key", "htmlBody", "label", "Body (HTML)", "type", "textarea", "required", true,
                                   "placeholder", "<p>Hello {{name}}, your task is done.</p>", "helpText", "Email body (supports HTML)"),
                            Map.of("key", "from", "label", "From", "type", "text", "required", false,
                                   "placeholder", "noreply@crescendo.run", "helpText", "Sender address (default: your domain)"),
                            Map.of("key", "cc", "label", "CC", "type", "text", "required", false,
                                   "helpText", "CC recipients (comma-separated)"),
                            Map.of("key", "bcc", "label", "BCC", "type", "text", "required", false,
                                   "helpText", "BCC recipients (comma-separated)")))
                )
        ).credentialSchema(List.of()).category("communication").helpUrl("");
    }
}
