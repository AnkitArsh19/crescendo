package com.crescendo.apps.openai;

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

/**
 * Generates text using the OpenAI Chat Completions API.
 *
 * <p>Connection credentials: {@code apiKey} (sk-...)
 * <p>Config: {@code userPrompt} (required), {@code systemPrompt}, {@code model}, {@code temperature}, {@code maxTokens}
 */
@ActionMapping(appKey = "openai", actionKey = "chat-completion")
public class OpenAIChatCompletionHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIChatCompletionHandler.class);
    private static final String OPENAI_API = "https://api.openai.com/v1/chat/completions";

    private final RestClient restClient;

    public OpenAIChatCompletionHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String apiKey = creds != null ? (String) creds.get("apiKey") : null;
        if (apiKey == null || apiKey.isBlank()) {
            return ActionResult.failure("OpenAI requires an 'apiKey' in connection credentials");
        }

        String userPrompt = getRequired(config, "userPrompt");
        if (userPrompt == null) return ActionResult.failure("'userPrompt' is required");

        String model = config.getOrDefault("model", "gpt-4o-mini").toString();
        String systemPrompt = config.get("systemPrompt") != null ? config.get("systemPrompt").toString() : null;
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
                    .uri(OPENAI_API)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "openai");
            output.put("model", model);
            output.put("response", response);
            logger.info("[openai] Chat completion succeeded, model={}", model);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[openai] Chat completion failed: {}", e.getMessage());
            return ActionResult.failure("OpenAI request failed: " + e.getMessage());
        }
    }

    private String getRequired(Map<String, Object> config, String key) {
        Object val = config.get(key);
        return val != null ? val.toString() : null;
    }

    private double parseDouble(Object value, double defaultVal) {
        if (value == null) return defaultVal;
        try { return Double.parseDouble(value.toString()); }
        catch (NumberFormatException e) { return defaultVal; }
    }

    private int parseInt(Object value, int defaultVal) {
        if (value == null) return defaultVal;
        try { return Integer.parseInt(value.toString()); }
        catch (NumberFormatException e) { return defaultVal; }
    }
}
