package com.crescendo.apps.todoist;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

abstract class TodoistHandler implements ActionHandler {

    private final ObjectMapper objectMapper;

    TodoistHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ActionResult get(ActionContext context, String uri, Object... vars) {
        try {
            String response = client(context).get().uri(uri, vars).retrieve().body(String.class);
            return parsed(response);
        } catch (Exception e) {
            return ActionResult.failure("Todoist request failed: " + e.getMessage());
        }
    }

    ActionResult post(ActionContext context, Map<String, Object> body) {
        try {
            String response = client(context).post().uri("/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parsed(response);
        } catch (Exception e) {
            return ActionResult.failure("Todoist request failed: " + e.getMessage());
        }
    }

    Map<String, Object> body() {
        return new HashMap<>();
    }

    String value(ActionContext context, String key, String fallback) {
        Object value = context.configuration().get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private RestClient client(ActionContext context) {
        Object token = context.credentials() != null ? context.credentials().get("apiToken") : null;
        return RestClient.builder()
                .baseUrl("https://api.todoist.com/rest/v2")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + (token != null ? token : ""))
                .build();
    }

    private ActionResult parsed(String response) throws Exception {
        return ActionResult.success(Map.of("data", objectMapper.readValue(response, Object.class), "raw", response));
    }
}
