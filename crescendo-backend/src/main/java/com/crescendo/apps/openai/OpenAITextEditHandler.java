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
 * Text Edit — rewrites/edits input text according to an instruction.
 * The original /v1/edits endpoint was deprecated by OpenAI in Jan 2024;
 * this implementation uses the chat completions API instead (gpt-4o-mini default).
 */
@ActionMapping(appKey = "openai", actionKey = "text-edit")
public class OpenAITextEditHandler implements ActionHandler {

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String apiKey = creds != null ? String.valueOf(creds.getOrDefault("apiKey", "")) : "";
        if (apiKey.isBlank()) return ActionResult.failure("API Key is required");

        String input = config.get("input") != null ? config.get("input").toString() : null;
        String instruction = config.get("instruction") != null ? config.get("instruction").toString() : null;
        if (input == null || input.isBlank()) return ActionResult.failure("'input' is required");
        if (instruction == null || instruction.isBlank()) return ActionResult.failure("'instruction' is required");

        String model = config.getOrDefault("model", "gpt-4o-mini").toString();
        boolean simplify = Boolean.parseBoolean(config.getOrDefault("simplifyOutput", "true").toString());

        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a text editor. Apply the user's instruction to the provided text and return only the edited result."),
                            Map.of("role", "user", "content", instruction + "\n\nText:\n" + input)
                    )
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = RestClient.builder()
                    .baseUrl("https://api.openai.com/v1")
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .build()
                    .post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (simplify && response != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                    String content = msg != null ? msg.getOrDefault("content", "").toString() : "";
                    return ActionResult.success(Map.of("text", content));
                }
            }
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("OpenAI Text Edit failed: " + e.getMessage());
        }
    }
}
