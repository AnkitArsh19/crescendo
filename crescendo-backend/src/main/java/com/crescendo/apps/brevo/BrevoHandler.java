package com.crescendo.apps.brevo;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

abstract class BrevoHandler implements ActionHandler {

    private final ObjectMapper objectMapper;

    BrevoHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ActionResult post(ActionContext context, String uri, Object body) {
        try {
            String response = RestClient.builder()
                    .baseUrl("https://api.brevo.com/v3")
                    .defaultHeader("api-key", credential(context, "apiKey"))
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .build()
                    .post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of(
                    "data", response != null && !response.isBlank() ? objectMapper.readValue(response, Object.class) : Map.of(),
                    "raw", response != null ? response : ""
            ));
        } catch (Exception e) {
            return ActionResult.failure("Brevo request failed: " + e.getMessage());
        }
    }

    Object json(Object value, Object fallback) throws Exception {
        if (value == null) return fallback;
        if (value instanceof Map<?, ?> || value instanceof java.util.List<?>) return value;
        return objectMapper.readValue(String.valueOf(value), Object.class);
    }

    String value(ActionContext context, String key, String fallback) {
        Object value = context.configuration().get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private String credential(ActionContext context, String key) {
        Object value = context.credentials() != null ? context.credentials().get(key) : null;
        return value == null ? "" : String.valueOf(value);
    }
}
