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
                "Receive and send webhooks — trigger workflows from external services or POST data out",
                "/icons/webhook.svg", AuthType.NONE,
                List.of(
                    Map.of("triggerKey", "incoming", "name", "Incoming Webhook",
                        "description", "Triggers when the webhook endpoint receives a POST request",
                        "configSchema", List.of(
                            Map.of("key", "urlPattern", "label", "URL Pattern", "type", "text", "required", false,
                                   "placeholder", "/hooks/my-custom-endpoint", "helpText", "Optional custom URL path")))
                ),
                List.of(
                    Map.of("actionKey", "post-webhook", "name", "POST to Webhook",
                        "description", "Send a POST request to an external webhook",
                        "configSchema", List.of(
                            Map.of("key", "url", "label", "Webhook URL", "type", "text", "required", true,
                                   "placeholder", "https://hooks.example.com/endpoint", "helpText", "Destination webhook URL"),
                            Map.of("key", "payload", "label", "Payload (JSON)", "type", "json", "required", false,
                                   "placeholder", "{\"event\": \"test\", \"data\": {}}", "helpText", "JSON body to send"),
                            Map.of("key", "headers", "label", "Custom Headers (JSON)", "type", "json", "required", false,
                                   "placeholder", "{\"X-Custom-Header\": \"value\"}", "helpText", "Optional headers")))
                )
        ).credentialSchema(List.of()).category("developer").helpUrl("");
    }
}
