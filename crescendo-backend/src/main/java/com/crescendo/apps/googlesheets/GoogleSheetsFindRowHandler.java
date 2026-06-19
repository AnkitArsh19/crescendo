package com.crescendo.apps.googlesheets;

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
 * Searches for a row by matching a column value.
 */
@ActionMapping(appKey = "google-sheets", actionKey = "find-row")
public class GoogleSheetsFindRowHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsFindRowHandler.class);
    private static final String SHEETS_API = "https://sheets.googleapis.com/v4/spreadsheets";

    private final RestClient restClient;

    public GoogleSheetsFindRowHandler() {
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
        String sheetName = str(config, "sheetName");
        String searchColumn = str(config, "searchColumn");
        String searchValue = str(config, "searchValue");
        if (spreadsheetId == null)
            return ActionResult.failure("'spreadsheetId' is required");
        if (searchColumn == null)
            return ActionResult.failure("'searchColumn' is required");
        if (searchValue == null)
            return ActionResult.failure("'searchValue' is required");

        String range = (sheetName != null ? sheetName : "Sheet1");
        logger.info("[google-sheets] Finding row: column='{}', value='{}' in '{}'", searchColumn, searchValue,
                spreadsheetId);

        try {
            // Read all rows
            String url = SHEETS_API + "/" + spreadsheetId + "/values/" + range;
            Map<String, Object> response = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            List<List<Object>> rows = (List<List<Object>>) response.get("values");
            if (rows == null || rows.isEmpty()) {
                return ActionResult.success(Map.of("found", false, "message", "No data in spreadsheet"));
            }

            // First row is headers
            List<Object> headers = rows.get(0);
            int colIndex = -1;
            for (int i = 0; i < headers.size(); i++) {
                if (searchColumn.equalsIgnoreCase(String.valueOf(headers.get(i)))) {
                    colIndex = i;
                    break;
                }
            }
            if (colIndex == -1) {
                return ActionResult.failure("Column '" + searchColumn + "' not found in headers: " + headers);
            }

            // Search rows
            for (int r = 1; r < rows.size(); r++) {
                List<Object> row = rows.get(r);
                if (colIndex < row.size() && searchValue.equals(String.valueOf(row.get(colIndex)))) {
                    Map<String, Object> rowData = new HashMap<>();
                    for (int c = 0; c < headers.size() && c < row.size(); c++) {
                        rowData.put(String.valueOf(headers.get(c)), row.get(c));
                    }
                    Map<String, Object> output = new HashMap<>();
                    output.put("found", true);
                    output.put("rowIndex", r + 1); // 1-based
                    output.put("rowData", rowData);
                    return ActionResult.success(output);
                }
            }

            return ActionResult.success(Map.of("found", false, "message", "No matching row found"));

        } catch (Exception e) {
            logger.error("[google-sheets] Find-row failed: {}", e.getMessage());
            return ActionResult.failure("Google Sheets find-row failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }
}
