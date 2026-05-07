package com.crescendo.apps.sarvam;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ActionMapping(appKey = "sarvam", actionKey = "chat-completion")
public class SarvamChatCompletionHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SarvamChatCompletionHandler.class);
    private static final String SARVAM_API = "https://api.sarvam.ai/v1/chat/completions";

    private final RestClient restClient;

    public SarvamChatCompletionHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String apiKey = creds != null ? asString(creds.get("apiKey")) : null;
        if (apiKey == null || apiKey.isBlank()) {
            return ActionResult.failure("Sarvam AI requires an 'apiKey' in connection credentials");
        }

        String userPrompt = asString(config.get("userPrompt"));
        if (userPrompt == null || userPrompt.isBlank()) {
            return ActionResult.failure("'userPrompt' is required");
        }

        String model = defaultIfBlank(asString(config.get("model")), "sarvam-m");
        String systemPrompt = asString(config.get("systemPrompt"));
        double temperature = parseDouble(config.get("temperature"), 0.7);
        int maxTokens = parseInt(config.get("maxTokens"), 1024);

        try {
            List<Map<String, String>> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }
            messages.add(Map.of("role", "user", "content", userPrompt));

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("messages", messages);
            payload.put("temperature", temperature);
            payload.put("max_tokens", maxTokens);

            String response = restClient.post()
                    .uri(SARVAM_API)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "sarvam");
            output.put("model", model);
            output.put("response", response);
            logger.info("[sarvam] Chat completion succeeded, model={}", model);
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[sarvam] Chat completion failed", e);
            return ActionResult.failure("Sarvam AI request failed: " + e.getMessage());
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