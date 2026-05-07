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

/**
 * Updates an existing row in a worksheet by row index.
 */
@ActionMapping(appKey = "microsoft-excel", actionKey = "update-row")
public class MicrosoftExcelUpdateRowHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(MicrosoftExcelUpdateRowHandler.class);
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
        String worksheetId = asString(config.get("worksheetId"));
        String rowIndexStr = asString(config.get("rowIndex"));
        Object valuesObj = config.get("values");

        if (driveItemId == null || driveItemId.isBlank()) return ActionResult.failure("'driveItemId' is required");
        if (worksheetId == null || worksheetId.isBlank()) return ActionResult.failure("'worksheetId' is required");
        if (rowIndexStr == null || rowIndexStr.isBlank()) return ActionResult.failure("'rowIndex' is required");

        int rowIndex;
        try {
            rowIndex = Integer.parseInt(rowIndexStr);
        } catch (NumberFormatException e) {
            return ActionResult.failure("'rowIndex' must be a number");
        }

        if (!(valuesObj instanceof List<?> values) || values.isEmpty()) {
            return ActionResult.failure("'values' must be a non-empty array");
        }

        try {
            // Graph API: PATCH /me/drive/items/{id}/workbook/worksheets/{sheet}/range(address='A{row}:Z{row}')
            String range = "A" + rowIndex + ":Z" + rowIndex;
            String uri = GRAPH_API + "/me/drive/items/" + driveItemId
                    + "/workbook/worksheets/" + worksheetId
                    + "/range(address='" + range + "')";

            String response = RestClient.create()
                    .patch()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("values", List.of(values)))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "microsoft-excel");
            output.put("action", "update-row");
            output.put("rowIndex", rowIndex);
            output.put("response", response);
            logger.info("[microsoft-excel] Row updated successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[microsoft-excel] Update row failed", e);
            return ActionResult.failure("Update row failed: " + e.getMessage());
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}
