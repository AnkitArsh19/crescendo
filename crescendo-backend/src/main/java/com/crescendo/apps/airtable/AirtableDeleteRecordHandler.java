package com.crescendo.apps.airtable;

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

/**
 * Deletes a record from Airtable via DELETE /v0/{baseId}/{tableId}/{recordId}.
 */
@ActionMapping(appKey = "airtable", actionKey = "delete-record")
public class AirtableDeleteRecordHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(AirtableDeleteRecordHandler.class);
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

        logger.info("[airtable] Deleting record '{}' from {}/{}", recordId, baseId, tableId);

        try {
            String url = AIRTABLE_API + "/" + baseId + "/" + tableId + "/" + recordId;
            Map<String, Object> response = restClient.delete()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "airtable");
            output.put("action", "delete-record");
            output.put("recordId", recordId);
            output.put("deleted", response != null ? response.get("deleted") : true);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[airtable] Delete record failed: {}", e.getMessage());
            return ActionResult.failure("Airtable delete-record failed: " + e.getMessage());
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
