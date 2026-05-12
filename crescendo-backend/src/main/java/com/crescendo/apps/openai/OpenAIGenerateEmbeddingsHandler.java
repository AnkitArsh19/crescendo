package com.crescendo.apps.openai;

import com.crescendo.execution.action.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import java.util.*;

@ActionMapping(appKey = "openai", actionKey = "generate-embeddings")
public class OpenAIGenerateEmbeddingsHandler implements ActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(OpenAIGenerateEmbeddingsHandler.class);
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();
        String apiKey = creds != null ? (String) creds.get("apiKey") : null;
        if (apiKey == null) return ActionResult.failure("OpenAI requires 'apiKey'");

        String input = config.get("input") != null ? config.get("input").toString() : null;
        if (input == null) return ActionResult.failure("'input' is required");
        String model = config.getOrDefault("model", "text-embedding-3-small").toString();

        try {
            Map<String, Object> resp = restClient.post()
                    .uri("https://api.openai.com/v1/embeddings")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("model", model, "input", input))
                    .retrieve().body(Map.class);

            Map<String, Object> out = new HashMap<>();
            out.put("provider", "openai");
            out.put("action", "generate-embeddings");
            if (resp != null && resp.containsKey("data")) {
                var data = (List<Map<String, Object>>) resp.get("data");
                if (!data.isEmpty()) out.put("embedding", data.get(0).get("embedding"));
            }
            return ActionResult.success(out);
        } catch (Exception e) {
            return ActionResult.failure("OpenAI embeddings failed: " + e.getMessage());
        }
    }
}
