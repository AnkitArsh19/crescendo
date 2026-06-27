package com.crescendo.apps.hubspot;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles HubSpot Form operations.
 *
 * Operations (from n8n V2/FormDescription.ts):
 *   - form:getFields — Get all fields from a form
 *   - form:submit    — Submit data to a HubSpot form (uses Forms API v3)
 */
@Component
public class HubSpotFormHandlers {

    private static final String FORMS_API = "https://api.hsforms.com/submissions/v3/integration/submit";
    private static final String FORMS_LIST_API = "https://api.hubapi.com/marketing/v3/forms";

    @ActionMapping(appKey = "hubspot", actionKey = "form-getFields")
    public Object getFormFields(ActionContext context) throws Exception {
        String formId = context.getString("formId");

        // Fetch form definition; fields are embedded in the form object
        return RestClient.builder()
                .url(FORMS_LIST_API + "/" + formId)
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "form-submit")
    public Object submitForm(ActionContext context) throws Exception {
        String formId = context.getString("formId");
        String portalId = context.getString("portalId");   // HubSpot portal/account ID

        // Fields submitted to the form
        Map<String, Object> fields = context.getMap("fields");
        List<Map<String, Object>> fieldsList = new java.util.ArrayList<>();
        if (fields != null) {
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                fieldsList.add(Map.of("objectTypeId", "0-1", "name", entry.getKey(), "value", entry.getValue()));
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("fields", fieldsList);

        // Optional context (hutk, pageUri, pageName)
        Map<String, Object> ctx = context.getMap("context");
        if (ctx != null && !ctx.isEmpty()) body.put("context", ctx);

        // Optional: skipValidation
        Boolean skipValidation = context.getBoolean("skipValidation");
        if (skipValidation != null) body.put("skipValidation", skipValidation);

        return RestClient.builder()
                .url(FORMS_API + "/" + portalId + "/" + formId)
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }
}
