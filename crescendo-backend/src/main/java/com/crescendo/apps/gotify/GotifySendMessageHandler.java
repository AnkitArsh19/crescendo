package com.crescendo.apps.gotify;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

@ActionMapping(appKey = "gotify", actionKey = "send-message")
public class GotifySendMessageHandler implements ActionHandler {

    private final ObjectMapper objectMapper;

    public GotifySendMessageHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
public ActionResult execute(ActionContext context) {
        try {
            String message = value(context.configuration(), "message", "");
            if (message.isBlank()) return ActionResult.failure("Gotify message is required");
            String response = RestClient.create(trim(value(context.credentials(), "baseUrl", "")))
                    .post()
                    .uri("/message?token={token}", value(context.credentials(), "appToken", ""))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "title", value(context.configuration(), "title", ""),
                            "message", message,
                            "priority", intValue(context.configuration().get("priority"), 5)
                    ))
                    .retrieve()
                    .body(String.class);
            return parsed(response);
        } catch (Exception e) {
            return ActionResult.failure("Gotify send message failed: " + e.getMessage());
        }
    }

    private ActionResult parsed(String response) throws Exception {
        return ActionResult.success(Map.of("data", objectMapper.readValue(response, Object.class), "raw", response));
    }

    private String value(Map<String, Object> map, String key, String fallback) {
        Object value = map != null ? map.get(key) : null;
        return value == null ? fallback : String.valueOf(value);
    }

    private int intValue(Object value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String trim(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
