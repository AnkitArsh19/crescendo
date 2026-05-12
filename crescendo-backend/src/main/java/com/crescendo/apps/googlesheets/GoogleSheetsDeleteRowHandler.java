package com.crescendo.apps.googlesheets;

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
 * Deletes a row from a Google Sheet by row number using batchUpdate (deleteRange).
 */
@ActionMapping(appKey = "google-sheets", actionKey = "delete-row")
public class GoogleSheetsDeleteRowHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsDeleteRowHandler.class);
    private static final String SHEETS_API = "https://sheets.googleapis.com/v4/spreadsheets";

    private final RestClient restClient;

    public GoogleSheetsDeleteRowHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = creds != null ? (String) creds.get("accessToken") : null;
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Google Sheets requires an 'accessToken'");
        }

        String spreadsheetId = str(config, "spreadsheetId");
        String rowIndexStr = str(config, "rowIndex");
        if (spreadsheetId == null) return ActionResult.failure("'spreadsheetId' is required");
        if (rowIndexStr == null) return ActionResult.failure("'rowIndex' is required");

        int rowIndex;
        try {
            rowIndex = Integer.parseInt(rowIndexStr);
        } catch (NumberFormatException e) {
            return ActionResult.failure("'rowIndex' must be a number");
        }

        // Get sheetId (numeric) — first get spreadsheet metadata
        String sheetName = str(config, "sheetName");
        logger.info("[google-sheets] Deleting row {} from spreadsheet '{}'", rowIndex, spreadsheetId);

        try {
            // Get sheet ID (the numeric ID, not the name)
            int sheetId = 0;
            if (sheetName != null && !sheetName.isBlank()) {
                String metaUrl = SHEETS_API + "/" + spreadsheetId + "?fields=sheets.properties";
                Map<String, Object> meta = restClient.get()
                        .uri(metaUrl)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .body(Map.class);

                var sheets = (java.util.List<Map<String, Object>>) meta.get("sheets");
                if (sheets != null) {
                    for (var sheet : sheets) {
                        var props = (Map<String, Object>) sheet.get("properties");
                        if (props != null && sheetName.equals(props.get("title"))) {
                            sheetId = ((Number) props.get("sheetId")).intValue();
                            break;
                        }
                    }
                }
            }

            // Build batchUpdate request to delete the row
            Map<String, Object> deleteRequest = Map.of(
                "requests", java.util.List.of(
                    Map.of("deleteDimension", Map.of(
                        "range", Map.of(
                            "sheetId", sheetId,
                            "dimension", "ROWS",
                            "startIndex", rowIndex - 1, // 0-based
                            "endIndex", rowIndex          // exclusive
                        )
                    ))
                )
            );

            String url = SHEETS_API + "/" + spreadsheetId + ":batchUpdate";
            String response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(deleteRequest)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-sheets");
            output.put("action", "delete-row");
            output.put("spreadsheetId", spreadsheetId);
            output.put("deletedRow", rowIndex);
            output.put("response", response);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[google-sheets] Delete-row failed: {}", e.getMessage());
            return ActionResult.failure("Google Sheets delete-row failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }
}
