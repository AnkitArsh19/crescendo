package com.crescendo.apps.wikipedia;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;

import java.util.Map;

abstract class WikipediaHandler implements ActionHandler {

    private final ObjectMapper objectMapper;

    WikipediaHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ActionResult get(String language, String uri, Object... uriVariables) {
        try {
            String response = RestClient.create("https://" + language + ".wikipedia.org").get()
                    .uri(uri, uriVariables)
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of(
                    "data", objectMapper.readValue(response, Object.class),
                    "raw", response != null ? response : ""
            ));
        } catch (Exception e) {
            return ActionResult.failure("Wikipedia request failed: " + e.getMessage());
        }
    }

    String value(ActionContext context, String key, String fallback) {
        Object value = context.configuration().get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        return String.valueOf(value).trim();
    }
}
