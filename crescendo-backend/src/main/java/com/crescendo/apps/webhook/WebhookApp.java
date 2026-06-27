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
        return new App("crescendo-webhook", "Webhook", """
                Webhooks allow you to connect external applications that aren't natively supported. The Crescendo Webhook app lets you receive incoming data payloads from any service or send POST requests outward.

                **What you can do with Webhooks in Crescendo:**
                - Trigger a workflow when a payment gateway (like Stripe or Razorpay) sends an event
                - Process form submissions from your website or landing page
                - Send data to external services securely
                - Act as an intermediary to transform payloads between two systems

                **Actions available:**
                - Send Webhook — dispatch a customizable HTTP POST request to a destination

                **Triggers available:**
                - Catch Webhook — generate a unique URL to receive incoming HTTP payloads

                **Who should use this:** Developers, operations teams, and power users needing to integrate custom internal tools or third-party APIs.

                **Authentication:** None natively (handles inbound/outbound unauthenticated requests or custom headers).
                """,
                "/icons/webhook.svg", AuthType.NONE,
                List.of(
                    Map.of("triggerKey", "incoming", "name", "Incoming Webhook",
                        "description", "Triggers when the webhook endpoint receives an HTTP request",
                        "configSchema", List.of(
                            Map.of("key", "multipleMethods", "label", "Allow Multiple HTTP Methods", "type", "boolean", "required", false,
                                   "helpText", "Whether to allow the webhook to listen for multiple HTTP methods"),
                            Map.of("key", "method", "label", "HTTP Method", "type", "select", "required", false,
                                   "options", List.of(
                                           Map.of("value", "POST", "label", "POST"),
                                           Map.of("value", "GET", "label", "GET"),
                                           Map.of("value", "PUT", "label", "PUT"),
                                           Map.of("value", "PATCH", "label", "PATCH"),
                                           Map.of("value", "DELETE", "label", "DELETE"),
                                           Map.of("value", "HEAD", "label", "HEAD")),
                                   "helpText", "HTTP method to listen for (used if multiple methods is disabled)"),
                            Map.of("key", "httpMethods", "label", "HTTP Methods", "type", "select", "required", false,
                                   "options", List.of(
                                           Map.of("value", "POST", "label", "POST"),
                                           Map.of("value", "GET", "label", "GET"),
                                           Map.of("value", "PUT", "label", "PUT"),
                                           Map.of("value", "PATCH", "label", "PATCH"),
                                           Map.of("value", "DELETE", "label", "DELETE"),
                                           Map.of("value", "HEAD", "label", "HEAD")),
                                   "helpText", "Select multiple methods to listen to"),
                            Map.of("key", "urlPattern", "label", "URL Pattern", "type", "text", "required", false,
                                   "placeholder", "/hooks/my-custom-endpoint", "helpText", "Optional custom URL path"),
                            Map.of("key", "responseMode", "label", "Response Mode", "type", "select", "required", false,
                                   "options", List.of(
                                           Map.of("value", "immediate", "label", "Reply Immediately"),
                                           Map.of("value", "wait", "label", "Wait for Respond Step"),
                                           Map.of("value", "last-step", "label", "Wait for Last Step Output")),
                                   "helpText", "Wait modes return workflow output if the workflow finishes before timeout"),
                            Map.of("key", "timeoutSeconds", "label", "Wait Timeout Seconds", "type", "text", "required", false,
                                   "placeholder", "25", "helpText", "1 to 60 seconds"),
                            Map.of("key", "responseCode", "label", "Response Code", "type", "number", "required", false,
                                   "placeholder", "200", "helpText", "The HTTP response code to return (used for Immediate mode)"),
                            Map.of("key", "responseData", "label", "Response Data", "type", "textarea", "required", false,
                                   "placeholder", "{\"status\": \"ok\"}", "helpText", "Custom response payload (used for Immediate mode)"),
                            Map.of("key", "authentication", "label", "Authentication", "type", "select", "required", false,
                                   "options", List.of(
                                           Map.of("value", "none", "label", "None"),
                                           Map.of("value", "basicAuth", "label", "Basic Auth"),
                                           Map.of("value", "headerAuth", "label", "Header Auth")
                                   ), "helpText", "Require authentication to trigger this webhook"),
                            Map.of("key", "options", "label", "Options (JSON)", "type", "json", "required", false,
                                   "placeholder", "{\"ignoreBots\": true, \"ipWhitelist\": \"192.168.1.1\", \"binaryData\": false, \"rawBody\": false}",
                                   "helpText", "Advanced webhook options")))
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
