package com.crescendo.apps.gemini;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ActionMapping(appKey = "gemini", actionKey = "text-message")
public class GeminiTextHandler implements ActionHandler {

    private final tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();

    @Value("${gemini.api.key:}")
    private String platformApiKey;

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String apiKey = creds != null ? String.valueOf(creds.get("apiKey")) : null;
        if (apiKey == null || apiKey.isBlank() || "null".equalsIgnoreCase(apiKey)) {
            apiKey = platformApiKey;
        }
        if (apiKey == null || apiKey.isBlank()) {
            return ActionResult.failure("API Key is required. Connect a Gemini account or set gemini.api.key in application.properties.");
        }

        RestClient client = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String model = config.getOrDefault("model", "gemini-2.5-flash").toString();
        if (model.startsWith("models/")) {
            model = model.substring("models/".length());
        }
        // Map deprecated 1.5/1.0 names to active 2.5 models
        if ("gemini-1.5-flash".equalsIgnoreCase(model) || "gemini-flash".equalsIgnoreCase(model)) {
            model = "gemini-2.5-flash";
        } else if ("gemini-1.5-pro".equalsIgnoreCase(model) || "gemini-pro".equalsIgnoreCase(model)) {
            model = "gemini-2.5-pro";
        }

        String endpoint = "/" + model + ":generateContent?key=" + apiKey;

        Map<String, Object> part = Map.of("text", config.get("prompt"));
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(content));

        // Add generation configuration if requested
        Map<String, Object> generationConfig = new HashMap<>();
        if (config.containsKey("temperature")) {
            generationConfig.put("temperature", config.get("temperature"));
        }
        if (config.containsKey("maxOutputTokens")) {
            generationConfig.put("maxOutputTokens", config.get("maxOutputTokens"));
        }
        if (!generationConfig.isEmpty()) {
            body.put("generationConfig", generationConfig);
        }

        try {
            String response = client.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            Object parsedResponse = mapper.readValue(response, Object.class);
            return ActionResult.success(parsedResponse);
        } catch (Exception e) {
            return ActionResult.failure("Gemini Text Generation failed: " + e.getMessage());
        }
    }
}
