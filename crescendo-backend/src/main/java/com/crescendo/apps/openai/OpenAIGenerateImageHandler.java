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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates an image using DALL-E via POST /v1/images/generations.
 */
@ActionMapping(appKey = "openai", actionKey = "generate-image")
public class OpenAIGenerateImageHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIGenerateImageHandler.class);
    private static final String OPENAI_API = "https://api.openai.com/v1/images/generations";
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String apiKey = creds != null ? (String) creds.get("apiKey") : null;
        if (apiKey == null || apiKey.isBlank()) {
            return ActionResult.failure("OpenAI requires an 'apiKey'");
        }

        String prompt = str(config, "prompt");
        if (prompt == null) return ActionResult.failure("'prompt' is required");

        String model = str(config, "model");
        if (model == null) model = "dall-e-3";
        String size = str(config, "size");
        if (size == null) size = "1024x1024";
        String quality = str(config, "quality");
        if (quality == null) quality = "standard";

        logger.info("[openai] Generating image: model='{}', size='{}'", model, size);

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("prompt", prompt);
            body.put("size", size);
            body.put("quality", quality);
            body.put("n", 1);

            Map<String, Object> response = restClient.post()
                    .uri(OPENAI_API)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "openai");
            output.put("action", "generate-image");
            output.put("model", model);

            if (response != null && response.containsKey("data")) {
                var data = (List<Map<String, Object>>) response.get("data");
                if (!data.isEmpty()) {
                    output.put("imageUrl", data.get(0).get("url"));
                    output.put("revisedPrompt", data.get(0).get("revised_prompt"));
                }
            }
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[openai] Generate image failed: {}", e.getMessage());
            return ActionResult.failure("OpenAI generate-image failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
