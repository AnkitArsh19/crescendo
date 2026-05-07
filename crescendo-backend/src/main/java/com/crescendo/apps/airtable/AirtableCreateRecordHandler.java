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

@ActionMapping(appKey = "airtable", actionKey = "create-record")
public class AirtableCreateRecordHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(AirtableCreateRecordHandler.class);
    private static final String AIRTABLE_API = "https://api.airtable.com/v0";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String token = resolveToken(creds);
        if (token == null) return ActionResult.failure("Airtable requires an 'accessToken' or 'apiKey' in connection credentials");

        String baseId = config.get("baseId") != null ? config.get("baseId").toString() : null;
        String tableId = config.get("tableId") != null ? config.get("tableId").toString() : null;
        Object fields = config.get("fields");

        if (baseId == null || baseId.isBlank()) return ActionResult.failure("'baseId' is required");
        if (tableId == null || tableId.isBlank()) return ActionResult.failure("'tableId' is required");
        if (fields == null) return ActionResult.failure("'fields' is required");

        try {
            String response = RestClient.create()
                    .post()
                    .uri(AIRTABLE_API + "/" + baseId + "/" + tableId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("fields", fields))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            logger.info("[airtable] Record created successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[airtable] Create record failed", e);
            return ActionResult.failure("Airtable create record failed: " + e.getMessage());
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
