package com.crescendo.apps.googletranslate;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Grouped handler for Google Translate operations.
 *
 * <p>Operations (mirrors n8n {@code GoogleTranslate.node.ts}):
 * <ul>
 *   <li>{@code translate} — translate text via Cloud Translation API v2</li>
 * </ul>
 *
 * <p>Credentials: {@code apiKey} (API key auth) OR {@code accessToken} (OAuth2)
 */
@Component
public class GoogleTranslateHandlers {

    private static final Logger logger = LoggerFactory.getLogger(GoogleTranslateHandlers.class);

    private static final String BASE = "https://translation.googleapis.com/language/translate/v2";

    // ── translate ─────────────────────────────────────────────────────────────

    /**
     * Translate text to a target language.
     * Config: text (required), translateTo (required, e.g. "fr", "de"),
     *         translateFrom (optional, auto-detect if absent),
     *         format (text|html, default "text")
     * Credentials: apiKey OR accessToken
     */
    @ActionMapping(appKey = "googletranslate", actionKey = "translate")
    @SuppressWarnings("unchecked")
    public ActionResult translate(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        // Support both API key and OAuth2 access token
        String apiKey = creds != null ? str(creds, "apiKey") : null;
        String accessToken = creds != null ? str(creds, "accessToken") : null;

        if ((apiKey == null || apiKey.isBlank()) && (accessToken == null || accessToken.isBlank())) {
            return ActionResult.failure("Google Translate requires either 'apiKey' or 'accessToken' in credentials");
        }

        String text = require(config, "text");
        if (text == null) return ActionResult.failure("'text' is required");
        String target = require(config, "translateTo");
        if (target == null) return ActionResult.failure("'translateTo' is required");

        String source = opt(config, "translateFrom", null);
        String format = opt(config, "format", "text");

        logger.info("[googletranslate] translate: target='{}', format='{}'", target, format);

        try {
            RestClient.Builder builder = RestClient.builder()
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

            if (accessToken != null && !accessToken.isBlank()) {
                builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            }
            RestClient client = builder.build();

            // Build request body
            Map<String, Object> body = new HashMap<>();
            body.put("q", text);
            body.put("target", target);
            body.put("format", format);
            if (source != null) body.put("source", source);

            // Append API key as query param if using key auth
            String uri = BASE + (apiKey != null && !apiKey.isBlank() ? "?key=" + apiKey : "");

            Map<String, Object> response = client.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googletranslate] translate failed: {}", e.getMessage());
            return ActionResult.failure("Google Translate failed: " + e.getMessage());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String require(Map<String, Object> config, String key) {
        Object v = config.get(key);
        return (v != null && !v.toString().isBlank()) ? v.toString() : null;
    }

    private String opt(Map<String, Object> config, String key, String defaultVal) {
        Object v = config.get(key);
        return (v != null && !v.toString().isBlank()) ? v.toString() : defaultVal;
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null && !v.toString().isBlank() ? v.toString() : null;
    }
}
