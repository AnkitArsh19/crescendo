package com.crescendo.apps.pushbullet;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

@ActionMapping(appKey = "pushbullet", actionKey = "send-note")
public class PushbulletSendNoteHandler implements ActionHandler {

    private final ObjectMapper objectMapper;

    public PushbulletSendNoteHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
public ActionResult execute(ActionContext context) {
        try {
            String body = value(context.configuration(), "body", "");
            if (body.isBlank()) return ActionResult.failure("Pushbullet body is required");
            String response = RestClient.builder()
                    .baseUrl("https://api.pushbullet.com/v2")
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + value(context.credentials(), "accessToken", ""))
                    .build()
                    .post()
                    .uri("/pushes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("type", "note", "title", value(context.configuration(), "title", ""), "body", body))
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("data", objectMapper.readValue(response, Object.class), "raw", response));
        } catch (Exception e) {
            return ActionResult.failure("Pushbullet send note failed: " + e.getMessage());
        }
    }

    private String value(Map<String, Object> map, String key, String fallback) {
        Object value = map != null ? map.get(key) : null;
        return value == null ? fallback : String.valueOf(value);
    }
}
