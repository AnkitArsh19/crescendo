package com.crescendo.apps.gemini;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ActionMapping(appKey = "gemini", actionKey = "image-analyze")
public class GeminiImageHandler implements ActionHandler {

    private final tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String apiKey = creds != null ? String.valueOf(creds.get("apiKey")) : null;
        if (apiKey == null || apiKey.isBlank()) return ActionResult.failure("API Key is required");

        RestClient client = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String model = config.getOrDefault("model", "gemini-1.5-pro").toString();
        String endpoint = "/" + model + ":generateContent?key=" + apiKey;

        // Note: Real implementations would download the image from imageUrl and convert to base64,
        // or use file API. Here we just provide a simplified stub for demonstration.
        Map<String, Object> textPart = Map.of("text", config.get("prompt"));
        Map<String, Object> content = Map.of("parts", List.of(textPart));
        
        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(content));

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
            return ActionResult.failure("Gemini Image Analysis failed: " + e.getMessage());
        }
    }
}
