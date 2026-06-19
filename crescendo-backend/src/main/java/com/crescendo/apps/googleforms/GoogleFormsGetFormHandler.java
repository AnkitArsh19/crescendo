package com.crescendo.apps.googleforms;

import com.crescendo.execution.action.*;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import java.util.*;

/**
 * Gets a Google Form's metadata via GET /v1/forms/{formId}.
 */
@ActionMapping(appKey = "google-forms", actionKey = "get-form")
@SuppressWarnings("unchecked")
public class GoogleFormsGetFormHandler implements ActionHandler {
    private final RestClient restClient = RestClient.create();

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();
        String token = creds != null ? (String) creds.get("accessToken") : null;
        if (token == null)
            return ActionResult.failure("Google Forms requires an OAuth2 accessToken");

        String formId = config.get("formId") != null ? config.get("formId").toString() : null;
        if (formId == null)
            return ActionResult.failure("'formId' is required");

        try {
            Map<String, Object> resp = restClient.get()
                    .uri("https://forms.googleapis.com/v1/forms/" + formId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);

            Map<String, Object> out = new HashMap<>();
            out.put("provider", "google-forms");
            out.put("action", "get-form");
            out.put("formId", resp != null ? resp.get("formId") : formId);
            out.put("title", resp != null ? resp.get("info") : null);
            out.put("responderUri", resp != null ? resp.get("responderUri") : null);
            return ActionResult.success(out);
        } catch (Exception e) {
            return ActionResult.failure("Google Forms get-form failed: " + e.getMessage());
        }
    }
}
