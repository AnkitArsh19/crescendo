package com.crescendo.apps.googleforms;

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

@ActionMapping(appKey = "google-forms", actionKey = "create-form")
public class GoogleFormsCreateFormHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleFormsCreateFormHandler.class);
    private static final String FORMS_API = "https://forms.googleapis.com/v1/forms";

    private final RestClient restClient;

    public GoogleFormsCreateFormHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = creds != null ? asString(creds.get("accessToken")) : null;
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Google Forms requires an 'accessToken' in connection credentials");
        }

        String title = asString(config.get("title"));
        if (title == null || title.isBlank()) {
            return ActionResult.failure("'title' is required");
        }

        Map<String, Object> info = new HashMap<>();
        info.put("title", title);
        if (config.get("documentTitle") != null) {
            info.put("documentTitle", config.get("documentTitle"));
        }

        try {
            String response = restClient.post()
                    .uri(FORMS_API)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("info", info))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-forms");
            output.put("response", response);
            logger.info("[google-forms] Form created successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[google-forms] Failed to create form", e);
            return ActionResult.failure("Google Forms create form failed: " + e.getMessage());
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}