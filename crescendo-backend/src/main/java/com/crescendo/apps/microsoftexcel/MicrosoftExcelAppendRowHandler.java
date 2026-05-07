package com.crescendo.apps.microsoftexcel;

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
import java.util.List;
import java.util.Map;

@ActionMapping(appKey = "microsoft-excel", actionKey = "append-row")
public class MicrosoftExcelAppendRowHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(MicrosoftExcelAppendRowHandler.class);
    private static final String GRAPH_API = "https://graph.microsoft.com/v1.0";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = asString(creds != null ? creds.get("accessToken") : null);
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Microsoft Excel requires 'accessToken' in connection credentials");
        }

        String driveItemId = asString(config.get("driveItemId"));
        String tableName = asString(config.get("tableName"));
        Object valuesObj = config.get("values");

        if (driveItemId == null || driveItemId.isBlank()) return ActionResult.failure("'driveItemId' is required");
        if (tableName == null || tableName.isBlank()) return ActionResult.failure("'tableName' is required");
        if (!(valuesObj instanceof List<?> values) || values.isEmpty()) {
            return ActionResult.failure("'values' must be a non-empty array");
        }

        try {
            String response = RestClient.create()
                    .post()
                    .uri(GRAPH_API + "/me/drive/items/" + driveItemId + "/workbook/tables/" + tableName + "/rows/add")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("values", List.of(values)))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "microsoft-excel");
            output.put("response", response);
            logger.info("[microsoft-excel] Row appended successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[microsoft-excel] Append row failed", e);
            return ActionResult.failure("Microsoft Excel append row failed: " + e.getMessage());
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}
