package com.crescendo.apps.http;

import com.crescendo.app.App;
import com.crescendo.apps.AppDefinition;
import com.crescendo.enums.AuthType;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class HttpApp implements AppDefinition {
    @Override
    public App toApp() {
        return new App("http", "HTTP / API", """
                The HTTP app is a universal connector that allows Crescendo to interact with almost any web service. Make standard REST API calls and receive incoming webhooks.

                **What you can do with HTTP in Crescendo:**
                - Connect to APIs that aren't natively supported in the app catalog
                - Receive incoming webhooks from external services (like payment gateways or forms)
                - Send data via POST/PUT requests with custom JSON bodies and headers
                - Handle pagination, multipart form uploads, and basic authentication

                **Actions available:**
                - HTTP Request — configure the URL, method, headers, and body
                - Catch Webhook — trigger a workflow when an external system sends a payload

                **Who should use this:** Developers and advanced users who need to integrate custom internal tools, third-party APIs, or unsupported platforms.

                **Authentication:** None natively (pass your own tokens via headers or query parameters).
                """,
                "/icons/http.png", AuthType.NONE,
                List.of(
                    Map.of("triggerKey", "catch-hook", "name", "Catch Webhook",
                        "description", "Receives inbound webhook payloads",
                        "configSchema", List.of(
                            Map.of("key", "childKey", "label", "Child Key", "type", "text", "required", false,
                                   "placeholder", "data.user", "helpText", "Parse specific JSON key path")))
                ),
                List.of(
                    Map.of("actionKey", "request", "name", "HTTP Request",
                        "description", "Send a GET, POST, PUT, PATCH, or DELETE request",
                        "configSchema", List.of(
                            Map.of("key", "authentication", "label", "Authentication", "type", "select", "required", true,
                                   "options", List.of(
                                        Map.of("value", "none", "label", "None"),
                                        Map.of("value", "basicAuth", "label", "Basic Auth"),
                                        Map.of("value", "bearerAuth", "label", "Bearer Auth"),
                                        Map.of("value", "headerAuth", "label", "Header Auth"),
                                        Map.of("value", "queryAuth", "label", "Query Auth"),
                                        Map.of("value", "digestAuth", "label", "Digest Auth"),
                                        Map.of("value", "oauth2", "label", "OAuth2")
                                   ), "helpText", "Authentication method"),
                            Map.of("key", "url", "label", "URL", "type", "text", "required", true,
                                   "placeholder", "https://api.example.com/data", "helpText", "Target URL"),
                            Map.of("key", "method", "label", "Method", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "GET", "label", "GET"),
                                       Map.of("value", "POST", "label", "POST"),
                                       Map.of("value", "PUT", "label", "PUT"),
                                       Map.of("value", "PATCH", "label", "PATCH"),
                                       Map.of("value", "DELETE", "label", "DELETE"),
                                       Map.of("value", "HEAD", "label", "HEAD"),
                                       Map.of("value", "OPTIONS", "label", "OPTIONS")
                                   ), "helpText", "HTTP method (default: GET)"),
                            Map.of("key", "sendQuery", "label", "Send Query Parameters", "type", "boolean", "required", false, "helpText", "Whether to send query parameters"),
                            Map.of("key", "specifyQuery", "label", "Specify Query", "type", "select", "required", false,
                                   "options", List.of(Map.of("value", "keypair", "label", "Using Fields"), Map.of("value", "json", "label", "Using JSON"))),
                            Map.of("key", "queryParameters", "label", "Query Parameters", "type", "json", "required", false, "helpText", "Key-Value pairs or JSON depending on Specify Query"),
                            Map.of("key", "sendHeaders", "label", "Send Headers", "type", "boolean", "required", false, "helpText", "Whether to send headers"),
                            Map.of("key", "specifyHeaders", "label", "Specify Headers", "type", "select", "required", false,
                                   "options", List.of(Map.of("value", "keypair", "label", "Using Fields"), Map.of("value", "json", "label", "Using JSON"))),
                            Map.of("key", "headerParameters", "label", "Headers", "type", "json", "required", false, "helpText", "Key-Value pairs or JSON depending on Specify Headers"),
                            Map.of("key", "sendBody", "label", "Send Body", "type", "boolean", "required", false, "helpText", "Whether to send a body"),
                            Map.of("key", "bodyType", "label", "Body Content Type", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "json", "label", "JSON"),
                                       Map.of("value", "raw", "label", "Raw"),
                                       Map.of("value", "form-urlencoded", "label", "Form URL Encoded"),
                                       Map.of("value", "multipart-form-data", "label", "Multipart Form Data"),
                                       Map.of("value", "binaryData", "label", "Binary Data")
                                   ), "helpText", "Body encoding"),
                            Map.of("key", "specifyBody", "label", "Specify Body", "type", "select", "required", false,
                                   "options", List.of(Map.of("value", "keypair", "label", "Using Fields"), Map.of("value", "json", "label", "Using JSON"))),
                            Map.of("key", "bodyParameters", "label", "Body Parameters / JSON", "type", "json", "required", false, "helpText", "Body data"),
                            Map.of("key", "rawBody", "label", "Raw Body", "type", "textarea", "required", false, "helpText", "Raw request body"),
                            Map.of("key", "options", "label", "Options (JSON)", "type", "json", "required", false,
                                   "placeholder", "{\"batching\": {\"batchSize\": 10}, \"proxy\": \"\", \"responseFormat\": \"autodetect\", \"fullResponse\": false, \"neverError\": false, \"redirect\": {\"followAllRedirects\": true}}",
                                   "helpText", "Advanced options (batching, proxy, response, redirect)"),
                            Map.of("key", "pagination", "label", "Pagination Options (JSON)", "type", "json", "required", false,
                                   "placeholder", "{\"paginationMode\": \"updateAParameterInEachRequest\", \"paginationCompleteWhen\": \"responseIsEmpty\"}",
                                   "helpText", "Advanced pagination settings")
                        ))
                )
        ).credentialSchema(List.of()).category("developer").helpUrl("");
    }
}
