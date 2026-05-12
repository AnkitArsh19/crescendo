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
        return new App("http", "HTTP / API", "Make HTTP requests to any REST API endpoint",
                "/icons/http.svg", AuthType.NONE,
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
                            Map.of("key", "url", "label", "URL", "type", "text", "required", true,
                                   "placeholder", "https://api.example.com/data", "helpText", "Target URL"),
                            Map.of("key", "method", "label", "Method", "type", "select", "required", false,
                                   "options", List.of(
                                       Map.of("value", "GET", "label", "GET"),
                                       Map.of("value", "POST", "label", "POST"),
                                       Map.of("value", "PUT", "label", "PUT"),
                                       Map.of("value", "PATCH", "label", "PATCH"),
                                       Map.of("value", "DELETE", "label", "DELETE")
                                   ), "helpText", "HTTP method (default: GET)"),
                            Map.of("key", "headers", "label", "Headers (JSON)", "type", "json", "required", false,
                                   "placeholder", "{\"Authorization\": \"Bearer xxx\"}", "helpText", "Request headers"),
                            Map.of("key", "body", "label", "Request Body", "type", "json", "required", false,
                                   "placeholder", "{\"key\": \"value\"}", "helpText", "JSON body for POST/PUT/PATCH"),
                            Map.of("key", "basicAuth", "label", "Basic Auth", "type", "text", "required", false,
                                   "placeholder", "username:password", "helpText", "Optional basic auth")))
                )
        ).credentialSchema(List.of()).category("developer").helpUrl("");
    }
}
