package com.crescendo.apps.airtable;

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

/**
 * Updates an existing Airtable record via PATCH /v0/{baseId}/{tableId}/{recordId}.
 */
@ActionMapping(appKey = "airtable", actionKey = "update-record")
public class AirtableUpdateRecordHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(AirtableUpdateRecordHandler.class);
    private static final String AIRTABLE_API = "https://api.airtable.com/v0";
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String token = resolveToken(creds);
        if (token == null) return ActionResult.failure("Airtable requires an API token");

        String baseId = str(config, "baseId");
        String tableId = str(config, "tableId");
        String recordId = str(config, "recordId");
        if (baseId == null) return ActionResult.failure("'baseId' is required");
        if (tableId == null) return ActionResult.failure("'tableId' is required");
        if (recordId == null) return ActionResult.failure("'recordId' is required");

        Object fieldsObj = config.get("fields");
        Map<String, Object> fields;
        if (fieldsObj instanceof Map<?,?> m) {
            fields = (Map<String, Object>) m;
        } else if (fieldsObj instanceof String s && !s.isBlank()) {
            // Parse JSON string — send as raw body instead
            logger.info("[airtable] Updating record '{}' in {}/{} with raw JSON fields", recordId, baseId, tableId);
            try {
                String url = AIRTABLE_API + "/" + baseId + "/" + tableId + "/" + recordId;
                String rawBody = "{\"fields\":" + s + "}";
                String response = restClient.patch()
                        .uri(url)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(rawBody)
                        .retrieve()
                        .body(String.class);

                Map<String, Object> output = new HashMap<>();
                output.put("provider", "airtable");
                output.put("action", "update-record");
                output.put("recordId", recordId);
                output.put("response", response);
                return ActionResult.success(output);
            } catch (Exception e) {
                return ActionResult.failure("Airtable update-record failed: " + e.getMessage());
            }
        } else {
            return ActionResult.failure("'fields' must be a JSON object or map");
        }

        logger.info("[airtable] Updating record '{}' in {}/{}", recordId, baseId, tableId);

        try {
            String url = AIRTABLE_API + "/" + baseId + "/" + tableId + "/" + recordId;
            Map<String, Object> response = restClient.patch()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("fields", fields))
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "airtable");
            output.put("action", "update-record");
            output.put("recordId", recordId);
            output.put("fields", response != null ? response.get("fields") : null);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[airtable] Update record failed: {}", e.getMessage());
            return ActionResult.failure("Airtable update-record failed: " + e.getMessage());
        }
    }

    private String resolveToken(Map<String, Object> creds) {
        if (creds == null) return null;
        String t = (String) creds.get("apiKey");
        if (t == null) t = (String) creds.get("accessToken");
        return (t != null && !t.isBlank()) ? t : null;
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? v.toString() : null;
    }
}
