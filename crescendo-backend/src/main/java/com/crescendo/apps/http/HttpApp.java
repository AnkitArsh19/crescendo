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
        return new App("http", "HTTP / API", "Make HTTP requests to any REST API",
                "/icons/http.svg", AuthType.NONE,
                List.of(),
                List.of(Map.of(
                    "actionKey", "request",
                    "name", "HTTP Request",
                    "description", "Send a GET, POST, PUT, PATCH, or DELETE request",
                    "configSchema", Map.of(
                        "url", "string (required) — target URL",
                        "method", "string — HTTP method (default: GET)",
                        "headers", "object — request headers",
                        "body", "string — request body (JSON)"
                    )
                ))
        )
        .credentialSchema(List.of())
        .category("developer")
        .helpUrl("");
    }
}
