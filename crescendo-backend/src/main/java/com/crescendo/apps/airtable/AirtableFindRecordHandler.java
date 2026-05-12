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
import java.util.List;
import java.util.Map;

/**
 * Finds a record in Airtable using filterByFormula via GET /v0/{baseId}/{tableId}.
 */
@ActionMapping(appKey = "airtable", actionKey = "find-record")
public class AirtableFindRecordHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(AirtableFindRecordHandler.class);
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
        if (baseId == null) return ActionResult.failure("'baseId' is required");
        if (tableId == null) return ActionResult.failure("'tableId' is required");

        String formula = str(config, "filterByFormula");
        String maxRecords = str(config, "maxRecords");
        if (maxRecords == null) maxRecords = "10";

        logger.info("[airtable] Finding records in {}/{} with formula '{}'", baseId, tableId, formula);

        try {
            StringBuilder url = new StringBuilder(AIRTABLE_API + "/" + baseId + "/" + tableId + "?maxRecords=" + maxRecords);
            if (formula != null && !formula.isBlank()) {
                url.append("&filterByFormula=").append(java.net.URLEncoder.encode(formula, "UTF-8"));
            }

            Map<String, Object> response = restClient.get()
                    .uri(url.toString())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> records = response != null
                    ? (List<Map<String, Object>>) response.get("records")
                    : List.of();

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "airtable");
            output.put("action", "find-record");
            output.put("resultCount", records.size());
            output.put("records", records);
            if (!records.isEmpty()) {
                output.put("firstRecordId", records.get(0).get("id"));
                output.put("firstRecordFields", records.get(0).get("fields"));
            }
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[airtable] Find record failed: {}", e.getMessage());
            return ActionResult.failure("Airtable find-record failed: " + e.getMessage());
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
