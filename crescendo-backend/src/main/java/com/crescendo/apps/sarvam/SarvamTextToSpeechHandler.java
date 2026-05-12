package com.crescendo.apps.sarvam;

import com.crescendo.execution.action.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import java.util.*;

/**
 * Converts text to speech using Sarvam AI TTS API.
 */
@ActionMapping(appKey = "sarvam", actionKey = "text-to-speech")
public class SarvamTextToSpeechHandler implements ActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(SarvamTextToSpeechHandler.class);
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();
        String apiKey = creds != null ? (String) creds.get("apiKey") : null;
        if (apiKey == null) return ActionResult.failure("Sarvam requires an 'apiKey'");

        String text = config.get("text") != null ? config.get("text").toString() : null;
        String lang = config.get("lang") != null ? config.get("lang").toString() : null;
        if (text == null) return ActionResult.failure("'text' is required");
        if (lang == null) return ActionResult.failure("'lang' is required");

        String speaker = config.getOrDefault("speaker", "meera").toString();

        try {
            Map<String, Object> body = Map.of(
                "inputs", List.of(text),
                "target_language_code", lang,
                "speaker", speaker
            );
            Map<String, Object> resp = restClient.post()
                    .uri("https://api.sarvam.ai/text-to-speech")
                    .header("api-subscription-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(Map.class);

            Map<String, Object> out = new HashMap<>();
            out.put("provider", "sarvam");
            out.put("action", "text-to-speech");
            out.put("audios", resp != null ? resp.get("audios") : null);
            return ActionResult.success(out);
        } catch (Exception e) {
            return ActionResult.failure("Sarvam TTS failed: " + e.getMessage());
        }
    }
}
