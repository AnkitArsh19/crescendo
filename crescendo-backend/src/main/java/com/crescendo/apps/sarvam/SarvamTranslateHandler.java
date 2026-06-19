package com.crescendo.apps.sarvam;

import com.crescendo.execution.action.*;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import java.util.*;

/**
 * Translates text between Indian languages using Sarvam AI translate API.
 */
@ActionMapping(appKey = "sarvam", actionKey = "translate")
@SuppressWarnings("unchecked")
public class SarvamTranslateHandler implements ActionHandler {
    private final RestClient restClient = RestClient.create();

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();
        String apiKey = creds != null ? (String) creds.get("apiKey") : null;
        if (apiKey == null)
            return ActionResult.failure("Sarvam requires an 'apiKey'");

        String text = config.get("text") != null ? config.get("text").toString() : null;
        String sourceLang = config.get("sourceLang") != null ? config.get("sourceLang").toString() : null;
        String targetLang = config.get("targetLang") != null ? config.get("targetLang").toString() : null;
        if (text == null)
            return ActionResult.failure("'text' is required");
        if (sourceLang == null)
            return ActionResult.failure("'sourceLang' is required");
        if (targetLang == null)
            return ActionResult.failure("'targetLang' is required");

        try {
            Map<String, Object> body = Map.of(
                    "input", text,
                    "source_language_code", sourceLang,
                    "target_language_code", targetLang);
            Map<String, Object> resp = restClient.post()
                    .uri("https://api.sarvam.ai/translate")
                    .header("api-subscription-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(Map.class);

            Map<String, Object> out = new HashMap<>();
            out.put("provider", "sarvam");
            out.put("action", "translate");
            out.put("translatedText", resp != null ? resp.get("translated_text") : null);
            return ActionResult.success(out);
        } catch (Exception e) {
            return ActionResult.failure("Sarvam translate failed: " + e.getMessage());
        }
    }
}
