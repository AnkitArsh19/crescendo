package com.crescendo.apps.gemini;

import com.crescendo.execution.action.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import java.util.*;

@ActionMapping(appKey = "gemini", actionKey = "analyze-image")
public class GeminiAnalyzeImageHandler implements ActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GeminiAnalyzeImageHandler.class);
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();
        String apiKey = creds != null ? (String) creds.get("apiKey") : null;
        if (apiKey == null) return ActionResult.failure("Gemini requires 'apiKey'");

        String imageUrl = config.get("imageUrl") != null ? config.get("imageUrl").toString() : null;
        String prompt = config.get("prompt") != null ? config.get("prompt").toString() : null;
        if (imageUrl == null) return ActionResult.failure("'imageUrl' is required");
        if (prompt == null) return ActionResult.failure("'prompt' is required");

        String model = config.getOrDefault("model", "gemini-2.0-flash").toString();
        logger.info("[gemini] Analyzing image with model '{}'", model);

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model
                    + ":generateContent?key=" + apiKey;

            Map<String, Object> body = Map.of("contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt),
                    Map.of("inline_data", Map.of("mime_type", "image/jpeg",
                            "file_uri", imageUrl))
                ))
            ));

            Map<String, Object> resp = restClient.post()
                    .uri(url).contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(Map.class);

            Map<String, Object> out = new HashMap<>();
            out.put("provider", "gemini");
            out.put("action", "analyze-image");
            if (resp != null && resp.containsKey("candidates")) {
                var candidates = (List<Map<String, Object>>) resp.get("candidates");
                if (!candidates.isEmpty()) {
                    var content = (Map<String, Object>) candidates.get(0).get("content");
                    var parts = (List<Map<String, Object>>) content.get("parts");
                    out.put("text", parts.get(0).get("text"));
                }
            }
            return ActionResult.success(out);
        } catch (Exception e) {
            return ActionResult.failure("Gemini analyze-image failed: " + e.getMessage());
        }
    }
}
