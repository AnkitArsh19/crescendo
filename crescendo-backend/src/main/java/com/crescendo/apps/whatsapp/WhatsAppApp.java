package com.crescendo.apps.whatsapp;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class WhatsAppApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("whatsapp", "WhatsApp Business", "Send messages via the WhatsApp Business Cloud API",
                "/icons/whatsapp.svg", AuthType.APIKEY,
                List.of(Map.of(
                    "triggerKey", "message-received",
                    "name", "Message Received",
                    "description", "Triggers when a WhatsApp message is received"
                )),
                List.of(Map.of(
                    "actionKey", "send-message",
                    "name", "Send Message",
                    "description", "Send a WhatsApp message to a phone number",
                    "configSchema", Map.of(
                        "to", "string (required) — recipient phone number with country code",
                        "message", "string (required) — message text"
                    )
                ))
        )
        .credentialSchema(List.of(
            Map.of("key", "accessToken", "label", "Permanent Access Token",
                    "type", "password", "required", true,
                    "placeholder", "EAAGZBq...",
                    "helpText", "Get a permanent token from the Meta for Developers portal → WhatsApp → API Setup"),
            Map.of("key", "phoneNumberId", "label", "Phone Number ID",
                    "type", "text", "required", true,
                    "placeholder", "123456789012345",
                    "helpText", "Your WhatsApp phone number ID from the Meta dashboard")
        ))
        .category("communication")
        .helpUrl("https://developers.facebook.com/apps/");
    }
}
