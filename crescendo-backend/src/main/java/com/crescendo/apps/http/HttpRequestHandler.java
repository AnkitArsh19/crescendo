package com.crescendo.apps.http;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic HTTP action handler — sends requests to any REST API.
 *
 * <p>Connection credentials (optional): {@code accessToken} or {@code apiKey}
 * <p>Config: {@code url} (required), {@code method}, {@code headers}, {@code body}
 */
@ActionMapping(appKey = "http", actionKey = "request")
public class HttpRequestHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);

    private final RestClient restClient;

    public HttpRequestHandler() {
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        if (config == null || !config.containsKey("url")) {
            return ActionResult.failure("HTTP action requires a 'url' in step configuration");
        }

        String url = config.get("url").toString();
        String method = config.getOrDefault("method", "GET").toString().toUpperCase();

        try {
            RestClient.RequestBodySpec spec = restClient.method(
                    org.springframework.http.HttpMethod.valueOf(method))
                    .uri(url);

            applyAuth(spec, context.credentials());
            applyHeaders(spec, config);

            String responseBody;
            if (hasBody(method) && config.containsKey("body")) {
                spec.contentType(MediaType.APPLICATION_JSON);
                spec.body(config.get("body"));
            }
            responseBody = spec.retrieve().body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("statusCode", 200);
            output.put("body", responseBody);
            logger.info("[http] {} {} completed successfully", method, url);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[http-action] Request failed: {} {}", method, url, e);
            return ActionResult.failure("HTTP request failed: " + e.getMessage());
        }
    }

    private void applyAuth(RestClient.RequestBodySpec spec, Map<String, Object> credentials) {
        if (credentials == null || credentials.isEmpty()) return;
        Object token = credentials.get("accessToken");
        if (token != null) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return;
        }
        Object apiKey = credentials.get("apiKey");
        if (apiKey != null) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
    }

    @SuppressWarnings("unchecked")
    private void applyHeaders(RestClient.RequestBodySpec spec, Map<String, Object> config) {
        Object headersObj = config.get("headers");
        if (headersObj instanceof Map<?, ?> headers) {
            ((Map<String, Object>) headers).forEach((k, v) ->
                    spec.header(k, v != null ? v.toString() : ""));
        }
    }

    private boolean hasBody(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }
}
