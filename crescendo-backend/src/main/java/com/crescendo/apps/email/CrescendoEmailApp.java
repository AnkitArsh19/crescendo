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
                "Send transactional emails using the built-in Crescendo Email Service — no external credentials needed",
                "/icons/crescendo-email.svg", AuthType.NONE,
                List.of(),
                List.of(Map.of(
                    "actionKey", "send-email",
                    "name", "Send Email",
                    "description", "Send an email using Crescendo's built-in email service",
                    "configSchema", Map.of(
                        "to", "string (required) — recipient email",
                        "subject", "string (required) — email subject",
                        "htmlBody", "string (required) — email body (HTML)",
                        "from", "string — sender address (default: your verified domain)"
                    )
                ))
        )
        .credentialSchema(List.of())
        .category("communication")
        .helpUrl("");
    }
}
