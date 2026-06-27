package com.crescendo.apps.openai;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Text Moderation — classifies whether text violates OpenAI content policy.
 * Uses the /v1/moderations endpoint (still active as of 2024).
 */
@ActionMapping(appKey = "openai", actionKey = "text-moderate")
public class OpenAITextModerateHandler implements ActionHandler {

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String apiKey = creds != null ? String.valueOf(creds.getOrDefault("apiKey", "")) : "";
        if (apiKey.isBlank()) return ActionResult.failure("API Key is required");

        String input = config.get("input") != null ? config.get("input").toString() : null;
        if (input == null || input.isBlank()) return ActionResult.failure("'input' is required");

        String model = config.getOrDefault("model", "omni-moderation-latest").toString();
        boolean simplify = Boolean.parseBoolean(config.getOrDefault("simplifyOutput", "true").toString());

        try {
            Map<String, Object> body = Map.of("model", model, "input", input);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = RestClient.builder()
                    .baseUrl("https://api.openai.com/v1")
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .build()
                    .post()
                    .uri("/moderations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (simplify && response != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                if (results != null && !results.isEmpty()) {
                    Map<String, Object> first = results.get(0);
                    return ActionResult.success(Map.of(
                            "flagged", first.getOrDefault("flagged", false),
                            "categories", first.getOrDefault("categories", Map.of()),
                            "categoryScores", first.getOrDefault("category_scores", Map.of())
                    ));
                }
            }
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("OpenAI Text Moderation failed: " + e.getMessage());
        }
    }
}
