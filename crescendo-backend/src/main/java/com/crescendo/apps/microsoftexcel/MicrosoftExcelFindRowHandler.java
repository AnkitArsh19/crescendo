package com.crescendo.apps.microsoftexcel;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds a row in a worksheet by searching a column for a specific value.
 */
@ActionMapping(appKey = "microsoft-excel", actionKey = "find-row")
public class MicrosoftExcelFindRowHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(MicrosoftExcelFindRowHandler.class);
    private static final String GRAPH_API = "https://graph.microsoft.com/v1.0";

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = asString(creds.get("accessToken"));
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Microsoft Excel requires 'accessToken'");
        }

        String driveItemId = asString(config.get("driveItemId"));
        String worksheetId = asString(config.get("worksheetId"));
        String searchColumn = asString(config.get("searchColumn"));
        String searchValue = asString(config.get("searchValue"));

        if (driveItemId == null || driveItemId.isBlank()) return ActionResult.failure("'driveItemId' is required");
        if (worksheetId == null || worksheetId.isBlank()) return ActionResult.failure("'worksheetId' is required");
        if (searchValue == null || searchValue.isBlank()) return ActionResult.failure("'searchValue' is required");

        try {
            // Get used range of worksheet
            String uri = GRAPH_API + "/me/drive/items/" + driveItemId
                    + "/workbook/worksheets/" + worksheetId + "/usedRange";

            Map<String, Object> rangeData = RestClient.create()
                    .get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            List<List<Object>> rows = (List<List<Object>>) rangeData.get("values");

            if (rows == null || rows.isEmpty()) {
                logger.info("[microsoft-excel] Row found successfully");
                return ActionResult.success(Map.of("found", false, "message", "Worksheet is empty"));
            }

            // Determine search column index
            int colIdx = 0; // default to first column
            if (searchColumn != null && !searchColumn.isBlank()) {
                try {
                    colIdx = Integer.parseInt(searchColumn);
                } catch (NumberFormatException e) {
                    // Try matching column header name
                    List<Object> headers = rows.get(0);
                    for (int i = 0; i < headers.size(); i++) {
                        if (searchColumn.equalsIgnoreCase(String.valueOf(headers.get(i)))) {
                            colIdx = i;
                            break;
                        }
                    }
                }
            }

            // Search rows
            for (int r = 0; r < rows.size(); r++) {
                List<Object> row = rows.get(r);
                if (colIdx < row.size() && searchValue.equals(String.valueOf(row.get(colIdx)))) {
                    Map<String, Object> output = new HashMap<>();
                    output.put("provider", "microsoft-excel");
                    output.put("action", "find-row");
                    output.put("found", true);
                    output.put("rowIndex", r + 1); // 1-based
                    output.put("rowData", row);
                    return ActionResult.success(output);
                }
            }

            return ActionResult.success(Map.of("found", false, "message",
                    "No row found with '" + searchValue + "' in column " + colIdx));

        } catch (Exception e) {
            logger.error("[microsoft-excel] Find row failed", e);
            return ActionResult.failure("Find row failed: " + e.getMessage());
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}
