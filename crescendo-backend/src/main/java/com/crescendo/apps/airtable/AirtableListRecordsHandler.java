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

@ActionMapping(appKey = "airtable", actionKey = "list-records")
public class AirtableListRecordsHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(AirtableListRecordsHandler.class);
    private static final String AIRTABLE_API = "https://api.airtable.com/v0";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String token = resolveToken(creds);
        if (token == null) return ActionResult.failure("Airtable requires an 'accessToken' or 'apiKey' in connection credentials");

        String baseId = config.get("baseId") != null ? config.get("baseId").toString() : null;
        String tableId = config.get("tableId") != null ? config.get("tableId").toString() : null;

        if (baseId == null || baseId.isBlank()) return ActionResult.failure("'baseId' is required");
        if (tableId == null || tableId.isBlank()) return ActionResult.failure("'tableId' is required");

        try {
            int maxRecords = 100;
            if (config.containsKey("maxRecords")) {
                try { maxRecords = Integer.parseInt(config.get("maxRecords").toString()); }
                catch (NumberFormatException ignored) {}
            }

            String uri = AIRTABLE_API + "/" + baseId + "/" + tableId + "?maxRecords=" + maxRecords;

            Object filter = config.get("filterByFormula");
            if (filter != null && !filter.toString().isBlank()) {
                uri += "&filterByFormula=" + filter;
            }

            String response = RestClient.create()
                    .get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[airtable] Records listed successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[airtable] List records failed", e);
            return ActionResult.failure("Airtable list records failed: " + e.getMessage());
        }
    }

    private String resolveToken(Map<String, Object> creds) {
        if (creds == null) return null;
        Object token = creds.get("accessToken");
        if (token != null) return token.toString();
        Object key = creds.get("apiKey");
        return key != null ? key.toString() : null;
    }
}
