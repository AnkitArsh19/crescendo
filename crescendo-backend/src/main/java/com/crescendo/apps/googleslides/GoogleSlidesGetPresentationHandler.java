package com.crescendo.apps.googleslides;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "google-slides", actionKey = "get-presentation")
public class GoogleSlidesGetPresentationHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSlidesGetPresentationHandler.class);
    private static final String SLIDES_API = "https://slides.googleapis.com/v1/presentations";

    private final RestClient restClient;

    public GoogleSlidesGetPresentationHandler() {
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

        String presentationId = asString(config.get("presentationId"));
        if (presentationId == null || presentationId.isBlank()) {
            return ActionResult.failure("'presentationId' is required");
        }

        try {
            String response = restClient.get()
                    .uri(SLIDES_API + "/" + presentationId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-slides");
            output.put("presentationId", presentationId);
            output.put("response", response);
            logger.info("[google-slides] Presentation fetched successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[google-slides] Failed to fetch presentation", e);
            return ActionResult.failure("Google Slides get presentation failed: " + e.getMessage());
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}