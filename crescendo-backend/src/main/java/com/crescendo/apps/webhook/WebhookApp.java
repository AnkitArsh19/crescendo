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
                                   "placeholder", "/hooks/my-custom-endpoint", "helpText", "Optional custom URL path"),
                            Map.of("key", "responseMode", "label", "Response Mode", "type", "select", "required", false,
                                   "options", List.of(
                                           Map.of("value", "immediate", "label", "Reply Immediately"),
                                           Map.of("value", "wait", "label", "Wait for Respond Step"),
                                           Map.of("value", "last-step", "label", "Wait for Last Step Output")),
                                   "helpText", "Wait modes return workflow output if the workflow finishes before timeout"),
                            Map.of("key", "timeoutSeconds", "label", "Wait Timeout Seconds", "type", "text", "required", false,
                                   "placeholder", "25", "helpText", "1 to 60 seconds")))
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
                                   "placeholder", "{\"X-Custom-Header\": \"value\"}", "helpText", "Optional headers"))),
                    Map.of("actionKey", "respond", "name", "Respond to Webhook",
                        "description", "Build a response payload for webhook/API workflows",
                        "configSchema", List.of(
                            Map.of("key", "status", "label", "Status", "type", "text", "required", false,
                                   "placeholder", "200"),
                            Map.of("key", "body", "label", "Body (JSON or Text)", "type", "json", "required", false,
                                   "placeholder", "{\"ok\": true}"),
                            Map.of("key", "headers", "label", "Headers (JSON)", "type", "json", "required", false,
                                   "placeholder", "{\"Content-Type\":\"application/json\"}")))
                )
        ).credentialSchema(List.of()).category("developer").helpUrl("");
    }
}
