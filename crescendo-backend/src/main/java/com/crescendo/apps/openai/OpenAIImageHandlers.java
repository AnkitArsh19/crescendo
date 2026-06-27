package com.crescendo.apps.openai;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "openai", actionKey = "image-create")
public class OpenAIImageHandlers implements ActionHandler {

    private final tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String apiKey = creds != null ? String.valueOf(creds.get("apiKey")) : null;
        if (apiKey == null || apiKey.isBlank()) return ActionResult.failure("API Key is required");

        RestClient client = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> body = new HashMap<>();
        body.put("prompt", config.get("prompt"));
        body.put("model", config.getOrDefault("model", "dall-e-2"));
        
        String responseFormat = String.valueOf(config.getOrDefault("responseFormat", "url"));
        body.put("response_format", responseFormat.equals("url") ? "url" : "b64_json");

        String[] optionalKeys = {"n", "quality", "size", "style"};
        for (String key : optionalKeys) {
            if (config.containsKey(key)) {
                body.put(key, config.get(key));
            }
        }

        try {
            String response = client.post()
                    .uri("/images/generations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            Object parsedResponse = mapper.readValue(response, Object.class);
            Object result = parsedResponse;

            // Simplify output mimic n8n behavior (root property: data)
            if (parsedResponse instanceof Map) {
                Map<?, ?> resMap = (Map<?, ?>) parsedResponse;
                if (resMap.containsKey("data")) {
                    result = Map.of("data", resMap.get("data"));
                }
            }

            return ActionResult.success(result);
        } catch (Exception e) {
            return ActionResult.failure("OpenAI Image Generation failed: " + e.getMessage());
        }
    }
}
