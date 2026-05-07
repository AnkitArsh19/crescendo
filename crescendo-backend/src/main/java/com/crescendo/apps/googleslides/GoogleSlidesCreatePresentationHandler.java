package com.crescendo.apps.googleslides;

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

@ActionMapping(appKey = "google-slides", actionKey = "create-presentation")
public class GoogleSlidesCreatePresentationHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSlidesCreatePresentationHandler.class);
    private static final String SLIDES_API = "https://slides.googleapis.com/v1/presentations";

    private final RestClient restClient;

    public GoogleSlidesCreatePresentationHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = asString(creds != null ? creds.get("accessToken") : null);
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Google Slides requires an 'accessToken' in connection credentials");
        }

        String title = asString(config.get("title"));
        if (title == null || title.isBlank()) {
            return ActionResult.failure("'title' is required");
        }

        try {
            String response = restClient.post()
                    .uri(SLIDES_API)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("title", title))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-slides");
            output.put("response", response);
            logger.info("[google-slides] Presentation created successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[google-slides] Failed to create presentation", e);
            return ActionResult.failure("Google Slides create presentation failed: " + e.getMessage());
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}