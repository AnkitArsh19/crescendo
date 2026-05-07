package com.crescendo.apps.webhook;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class WebhookApp implements AppDefinition {

    @Override
    public App toApp() {
        return new App("crescendo-webhook", "Webhook",
                "Receive data from any external service via an HTTP webhook endpoint",
                "/icons/webhook.svg", AuthType.NONE,
                List.of(Map.of(
                    "triggerKey", "incoming",
                    "name", "Incoming Webhook",
                    "description", "Triggers when the webhook endpoint receives a POST request"
                )),
                List.of()
        )
        .credentialSchema(List.of())
        .category("developer")
        .helpUrl("");
    }
}
