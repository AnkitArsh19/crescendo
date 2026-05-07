package com.crescendo.apps.microsoftexcel;

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
 * Deletes a row from a worksheet by row index using the Graph API.
 */
@ActionMapping(appKey = "microsoft-excel", actionKey = "delete-row")
public class MicrosoftExcelDeleteRowHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(MicrosoftExcelDeleteRowHandler.class);
    private static final String GRAPH_API = "https://graph.microsoft.com/v1.0";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = asString(creds.get("accessToken"));
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Microsoft Excel requires 'accessToken'");
        }

        String driveItemId = asString(config.get("driveItemId"));
        String worksheetId = asString(config.get("worksheetId"));
        String rowIndexStr = asString(config.get("rowIndex"));

        if (driveItemId == null || driveItemId.isBlank()) return ActionResult.failure("'driveItemId' is required");
        if (worksheetId == null || worksheetId.isBlank()) return ActionResult.failure("'worksheetId' is required");
        if (rowIndexStr == null || rowIndexStr.isBlank()) return ActionResult.failure("'rowIndex' is required");

        int rowIndex;
        try {
            rowIndex = Integer.parseInt(rowIndexStr);
        } catch (NumberFormatException e) {
            return ActionResult.failure("'rowIndex' must be a number");
        }

        try {
            // Delete the row range: A{row}:ZZ{row}
            String range = "A" + rowIndex + ":ZZ" + rowIndex;
            String uri = GRAPH_API + "/me/drive/items/" + driveItemId
                    + "/workbook/worksheets/" + worksheetId
                    + "/range(address='" + range + "')/delete";

            RestClient.create()
                    .post()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(Map.of("shift", "Up"))
                    .retrieve()
                    .toBodilessEntity();

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "microsoft-excel");
            output.put("action", "delete-row");
            output.put("rowIndex", rowIndex);
            output.put("status", "deleted");
            logger.info("[microsoft-excel] Row deleted successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[microsoft-excel] Delete row failed", e);
            return ActionResult.failure("Delete row failed: " + e.getMessage());
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}
