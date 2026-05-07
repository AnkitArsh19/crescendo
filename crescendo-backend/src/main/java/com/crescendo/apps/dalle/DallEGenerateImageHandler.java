package com.crescendo.apps.dalle;

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
import java.util.Map;

@ActionMapping(appKey = "dall-e", actionKey = "generate-image")
public class DallEGenerateImageHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(DallEGenerateImageHandler.class);
    private static final String OPENAI_API = "https://api.openai.com/v1";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String apiKey = creds != null ? (String) creds.get("apiKey") : null;
        if (apiKey == null || apiKey.isBlank()) return ActionResult.failure("DALL-E requires 'apiKey' (OpenAI API key)");

        String prompt = config.get("prompt") != null ? config.get("prompt").toString() : null;
        if (prompt == null || prompt.isBlank()) return ActionResult.failure("'prompt' is required");

        String model = config.getOrDefault("model", "dall-e-3").toString();
        String size = config.getOrDefault("size", "1024x1024").toString();
        String quality = config.getOrDefault("quality", "standard").toString();
        int n = 1;
        if (config.containsKey("n")) {
            try { n = Integer.parseInt(config.get("n").toString()); }
            catch (NumberFormatException ignored) {}
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("prompt", prompt);
            body.put("size", size);
            body.put("n", n);
            if ("dall-e-3".equals(model)) {
                body.put("quality", quality);
            }

            String response = RestClient.create()
                    .post()
                    .uri(OPENAI_API + "/images/generations")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[dall-e] Image generated successfully, model={}", model);
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[dall-e] Generate image failed", e);
            return ActionResult.failure("DALL-E generate image failed: " + e.getMessage());
        }
    }
}
