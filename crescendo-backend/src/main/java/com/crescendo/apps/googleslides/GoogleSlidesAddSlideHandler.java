package com.crescendo.apps.googleslides;

import com.crescendo.execution.action.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import java.util.*;

/**
 * Adds a blank slide to a Google Slides presentation via batchUpdate.
 */
@ActionMapping(appKey = "google-slides", actionKey = "add-slide")
@SuppressWarnings("unchecked")
public class GoogleSlidesAddSlideHandler implements ActionHandler {
    private final RestClient restClient = RestClient.create();

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();
        String token = creds != null ? (String) creds.get("accessToken") : null;
        if (token == null)
            return ActionResult.failure("Google Slides requires an OAuth2 accessToken");

        String presentationId = config.get("presentationId") != null ? config.get("presentationId").toString() : null;
        if (presentationId == null)
            return ActionResult.failure("'presentationId' is required");

        String layout = config.getOrDefault("layout", "BLANK").toString();

        try {
            Map<String, Object> createSlideReq = Map.of(
                    "createSlide", Map.of(
                            "slideLayoutReference", Map.of("predefinedLayout", layout)));
            Map<String, Object> body = Map.of("requests", List.of(createSlideReq));

            Map<String, Object> resp = restClient.post()
                    .uri("https://slides.googleapis.com/v1/presentations/" + presentationId + ":batchUpdate")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(Map.class);

            Map<String, Object> out = new HashMap<>();
            out.put("provider", "google-slides");
            out.put("action", "add-slide");
            out.put("presentationId", presentationId);
            out.put("layout", layout);
            if (resp != null && resp.containsKey("replies")) {
                var replies = (List<Map<String, Object>>) resp.get("replies");
                if (!replies.isEmpty() && replies.get(0).containsKey("createSlide")) {
                    out.put("slideId", ((Map<?, ?>) replies.get(0).get("createSlide")).get("objectId"));
                }
            }
            return ActionResult.success(out);
        } catch (Exception e) {
            return ActionResult.failure("Google Slides add-slide failed: " + e.getMessage());
        }
    }
}
