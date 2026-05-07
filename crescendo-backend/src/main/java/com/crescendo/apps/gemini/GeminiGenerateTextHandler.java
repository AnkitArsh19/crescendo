package com.crescendo.apps.gemini;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ActionMapping(appKey = "gemini", actionKey = "generate-text")
public class GeminiGenerateTextHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GeminiGenerateTextHandler.class);
    private static final String GEMINI_API_ROOT = "https://generativelanguage.googleapis.com/v1beta/models/";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String apiKey = creds != null ? asString(creds.get("apiKey")) : null;
        if (apiKey == null || apiKey.isBlank()) {
            return ActionResult.failure("Gemini requires an 'apiKey' in connection credentials");
        }

        String userPrompt = asString(config.get("userPrompt"));
        if (userPrompt == null || userPrompt.isBlank()) {
            return ActionResult.failure("'userPrompt' is required");
        }

        String model = defaultIfBlank(asString(config.get("model")), "gemini-2.0-flash");
        String systemPrompt = asString(config.get("systemPrompt"));
        double temperature = parseDouble(config.get("temperature"), 0.7);
        int maxTokens = parseInt(config.get("maxTokens"), 1024);

        try {
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", temperature);
            generationConfig.put("maxOutputTokens", maxTokens);

            Map<String, Object> payload = new HashMap<>();
            payload.put("contents", List.of(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", userPrompt))
            )));
            payload.put("generationConfig", generationConfig);

            if (systemPrompt != null && !systemPrompt.isBlank()) {
                payload.put("systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                ));
            }

            String response = RestClient.create()
                    .post()
                    .uri(GEMINI_API_ROOT + model + ":generateContent?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "gemini");
            output.put("model", model);
            output.put("response", response);
            logger.info("[gemini] Text generated successfully, model={}", model);
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[gemini] Text generation failed", e);
            return ActionResult.failure("Gemini request failed: " + e.getMessage());
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private double parseDouble(Object value, double defaultVal) {
        if (value == null) return defaultVal;
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return defaultVal;
        }
    }

    private int parseInt(Object value, int defaultVal) {
        if (value == null) return defaultVal;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return defaultVal;
        }
    }
}
