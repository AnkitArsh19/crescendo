package com.crescendo.apps.googleslides;

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
import java.util.List;
import java.util.Map;

/**
 * Grouped handler for Google Slides operations.
 *
 * <p>Operations (mirrors n8n {@code GoogleSlides.node.ts}):
 * <ul>
 *   <li>{@code create} — presentations.create (POST /v1/presentations)</li>
 *   <li>{@code get}    — presentations.get    (GET  /v1/presentations/{id})</li>
 *   <li>{@code addSlide} — presentations.batchUpdate with createSlide request</li>
 * </ul>
 *
 * <p>Credentials: {@code accessToken} (OAuth2, presentations scope)
 */
@Component
public class GoogleSlidesHandlers {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSlidesHandlers.class);

    private static final String BASE = "https://slides.googleapis.com/v1/presentations";

    private final RestClient restClient;

    public GoogleSlidesHandlers() {
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ── create ────────────────────────────────────────────────────────────────

    /**
     * Create a new Google Slides presentation.
     * Config: title (required)
     */
    @ActionMapping(appKey = "googleslides", actionKey = "create")
    @SuppressWarnings("unchecked")
    public ActionResult create(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String title = require(config, "title");
        if (title == null) return ActionResult.failure("'title' is required");

        logger.info("[googleslides] create: title='{}'", title);

        try {
            Map<String, Object> response = restClient.post()
                    .uri(BASE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(Map.of("title", title))
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googleslides] create failed: {}", e.getMessage());
            return ActionResult.failure("Google Slides create failed: " + e.getMessage());
        }
    }

    // ── get ───────────────────────────────────────────────────────────────────

    /**
     * Get a Google Slides presentation by ID.
     * Config: presentationId (required)
     */
    @ActionMapping(appKey = "googleslides", actionKey = "get")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String presentationId = require(config, "presentationId");
        if (presentationId == null) return ActionResult.failure("'presentationId' is required");

        logger.info("[googleslides] get: presentationId='{}'", presentationId);

        try {
            Map<String, Object> response = restClient.get()
                    .uri(BASE + "/" + presentationId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googleslides] get failed: {}", e.getMessage());
            return ActionResult.failure("Google Slides get failed: " + e.getMessage());
        }
    }

    // ── addSlide ──────────────────────────────────────────────────────────────

    /**
     * Add a blank slide to an existing presentation.
     * Config: presentationId (required), layout (predefined layout name, default "BLANK")
     * <p>Valid layouts: BLANK, CAPTION_ONLY, TITLE, TITLE_AND_BODY, TITLE_AND_TWO_COLUMNS,
     * TITLE_ONLY, SECTION_HEADER, SECTION_TITLE_AND_DESCRIPTION, ONE_COLUMN_TEXT,
     * MAIN_POINT, BIG_NUMBER
     */
    @ActionMapping(appKey = "googleslides", actionKey = "addSlide")
    @SuppressWarnings("unchecked")
    public ActionResult addSlide(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String presentationId = require(config, "presentationId");
        if (presentationId == null) return ActionResult.failure("'presentationId' is required");
        String layout = opt(config, "layout", "BLANK");

        logger.info("[googleslides] addSlide: presentationId='{}', layout='{}'", presentationId, layout);

        try {
            Map<String, Object> body = Map.of(
                    "requests", List.of(Map.of(
                            "createSlide", Map.of(
                                    "slideLayoutReference", Map.of("predefinedLayout", layout)
                            )
                    ))
            );

            Map<String, Object> response = restClient.post()
                    .uri(BASE + "/" + presentationId + ":batchUpdate")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            // Extract created slideId from replies
            String slideId = null;
            if (response != null && response.containsKey("replies")) {
                var replies = (List<Map<String, Object>>) response.get("replies");
                if (!replies.isEmpty() && replies.get(0).containsKey("createSlide")) {
                    slideId = (String) ((Map<?, ?>) replies.get(0).get("createSlide")).get("objectId");
                }
            }

            Map<String, Object> out = new HashMap<>();
            out.put("presentationId", presentationId);
            out.put("layout", layout);
            if (slideId != null) out.put("slideId", slideId);
            return ActionResult.success(out);
        } catch (Exception e) {
            logger.error("[googleslides] addSlide failed: {}", e.getMessage());
            return ActionResult.failure("Google Slides addSlide failed: " + e.getMessage());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String resolveToken(ActionContext context) {
        Map<String, Object> creds = context.credentials();
        if (creds == null) return null;
        Object token = creds.get("accessToken");
        return token != null && !token.toString().isBlank() ? token.toString() : null;
    }

    private ActionResult missingToken() {
        return ActionResult.failure("Google Slides requires an OAuth2 accessToken in connection credentials");
    }

    private String require(Map<String, Object> config, String key) {
        Object v = config.get(key);
        return (v != null && !v.toString().isBlank()) ? v.toString() : null;
    }

    private String opt(Map<String, Object> config, String key, String defaultVal) {
        Object v = config.get(key);
        return (v != null && !v.toString().isBlank()) ? v.toString() : defaultVal;
    }
}
