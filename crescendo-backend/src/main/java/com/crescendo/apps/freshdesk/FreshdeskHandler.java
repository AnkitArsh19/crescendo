package com.crescendo.apps.freshdesk;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

abstract class FreshdeskHandler implements ActionHandler {

    private final ObjectMapper objectMapper;

    FreshdeskHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ActionResult get(ActionContext context, String uri, Object... vars) {
        try {
            String response = client(context).get().uri(uri, vars).retrieve().body(String.class);
            return parsed(response);
        } catch (Exception e) {
            return ActionResult.failure("Freshdesk request failed: " + e.getMessage());
        }
    }

    ActionResult post(ActionContext context, Object body) {
        try {
            String response = client(context).post().uri("/api/v2/tickets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parsed(response);
        } catch (Exception e) {
            return ActionResult.failure("Freshdesk request failed: " + e.getMessage());
        }
    }

    String value(ActionContext context, String key, String fallback) {
        Object value = context.configuration().get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    int intValue(ActionContext context, String key, int fallback) {
        try {
            return Integer.parseInt(value(context, key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private RestClient client(ActionContext context) {
        String domain = credential(context, "domain");
        String apiKey = credential(context, "apiKey") + ":X";
        return RestClient.builder()
                .baseUrl("https://" + domain)
                .defaultHeader(HttpHeaders.AUTHORIZATION,
                        "Basic " + Base64.getEncoder().encodeToString(apiKey.getBytes(StandardCharsets.UTF_8)))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private ActionResult parsed(String response) throws Exception {
        return ActionResult.success(Map.of("data", objectMapper.readValue(response, Object.class), "raw", response));
    }

    private String credential(ActionContext context, String key) {
        Object value = context.credentials() != null ? context.credentials().get(key) : null;
        return value == null ? "" : String.valueOf(value);
    }
}
