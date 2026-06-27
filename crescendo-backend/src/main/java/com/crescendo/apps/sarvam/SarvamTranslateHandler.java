package com.crescendo.apps.sarvam;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "sarvam", actionKey = "translate")
public class SarvamTranslateHandler implements ActionHandler {

    private final tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String apiKey = creds != null ? String.valueOf(creds.get("apiKey")) : null;
        if (apiKey == null || apiKey.isBlank()) return ActionResult.failure("API Key is required");

        RestClient client = RestClient.builder()
                .baseUrl("https://api.sarvam.ai")
                .defaultHeader("api-subscription-key", apiKey)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> body = new HashMap<>();
        body.put("input", config.get("text"));
        body.put("source_language_code", config.get("sourceLang"));
        body.put("target_language_code", config.get("targetLang"));
        
        if (config.containsKey("domain")) {
            body.put("domain", config.get("domain"));
        }

        try {
            String response = client.post()
                    .uri("/translate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            Object parsedResponse = mapper.readValue(response, Object.class);
            return ActionResult.success(parsedResponse);
        } catch (Exception e) {
            return ActionResult.failure("Sarvam Translate failed: " + e.getMessage());
        }
    }
}
