package com.crescendo.apps.googleforms;

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

@ActionMapping(appKey = "google-forms", actionKey = "list-responses")
public class GoogleFormsListResponsesHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleFormsListResponsesHandler.class);
    private static final String FORMS_API = "https://forms.googleapis.com/v1/forms";

    private final RestClient restClient;

    public GoogleFormsListResponsesHandler() {
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

        String formId = asString(config.get("formId"));
        if (formId == null || formId.isBlank()) {
            return ActionResult.failure("'formId' is required");
        }

        int pageSize = parseInt(config.get("pageSize"), 20);

        try {
            String response = restClient.get()
                    .uri(FORMS_API + "/" + formId + "/responses?pageSize=" + pageSize)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-forms");
            output.put("formId", formId);
            output.put("response", response);
            logger.info("[google-forms] Responses listed successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[google-forms] Failed to list responses", e);
            return ActionResult.failure("Google Forms list responses failed: " + e.getMessage());
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private int parseInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}