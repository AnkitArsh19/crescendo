package com.crescendo.apps.homeassistant;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

abstract class HomeAssistantHandler implements ActionHandler {

    private final ObjectMapper objectMapper;

    HomeAssistantHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ActionResult get(ActionContext context, String uri, Object... vars) {
        try {
            String response = client(context).get().uri(uri, vars).retrieve().body(String.class);
            return parsed(response);
        } catch (Exception e) {
            return ActionResult.failure("Home Assistant request failed: " + e.getMessage());
        }
    }

    ActionResult post(ActionContext context, String uri, Object body, Object... vars) {
        try {
            String response = client(context).post()
                    .uri(uri, vars)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body != null ? body : Map.of())
                    .retrieve()
                    .body(String.class);
            return parsed(response);
        } catch (Exception e) {
            return ActionResult.failure("Home Assistant request failed: " + e.getMessage());
        }
    }

    Object json(Object value) throws Exception {
        if (value == null) return Map.of();
        if (value instanceof Map<?, ?> || value instanceof java.util.List<?>) return value;
        return objectMapper.readValue(String.valueOf(value), Object.class);
    }

    String value(ActionContext context, String key) {
        Object value = context.configuration().get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private RestClient client(ActionContext context) {
        String baseUrl = credential(context, "baseUrl");
        String token = credential(context, "accessToken");
        return RestClient.builder()
                .baseUrl(trimTrailingSlash(baseUrl))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private ActionResult parsed(String response) throws Exception {
        return ActionResult.success(Map.of(
                "data", response != null && !response.isBlank() ? objectMapper.readValue(response, Object.class) : Map.of(),
                "raw", response != null ? response : ""
        ));
    }

    private String credential(ActionContext context, String key) {
        Object value = context.credentials() != null ? context.credentials().get(key) : null;
        return value == null ? "" : String.valueOf(value);
    }

    private String trimTrailingSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
