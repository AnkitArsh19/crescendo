package com.crescendo.apps.sarvam;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "sarvam", actionKey = "chat-completion")
public class SarvamChatCompletionHandler implements ActionHandler {

    private final tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String apiKey = creds != null ? String.valueOf(creds.get("apiKey")) : null;
        if (apiKey == null || apiKey.isBlank()) return ActionResult.failure("API Key is required");

        RestClient client = RestClient.builder()
                .baseUrl("https://api.sarvam.ai")
                .defaultHeader("api-subscription-key", apiKey)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> message = Map.of("role", "user", "content", config.get("prompt"));
        Map<String, Object> body = new HashMap<>();
        body.put("messages", java.util.List.of(message));
        body.put("model", config.getOrDefault("model", "saaras:v2"));

        try {
            String response = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            Object parsedResponse = mapper.readValue(response, Object.class);
            return ActionResult.success(parsedResponse);
        } catch (Exception e) {
            return ActionResult.failure("Sarvam Chat Completion failed: " + e.getMessage());
        }
    }
}
