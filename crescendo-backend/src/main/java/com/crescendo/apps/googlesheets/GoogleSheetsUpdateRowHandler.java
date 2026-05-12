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
import java.util.List;
import java.util.Map;

/**
 * Updates an existing row in a Google Sheet by row number.
 */
@ActionMapping(appKey = "google-sheets", actionKey = "update-row")
public class GoogleSheetsUpdateRowHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsUpdateRowHandler.class);
    private static final String SHEETS_API = "https://sheets.googleapis.com/v4/spreadsheets";

    private final RestClient restClient;

    public GoogleSheetsUpdateRowHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = creds != null ? (String) creds.get("accessToken") : null;
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Google Sheets requires an 'accessToken'");
        }

        String spreadsheetId = str(config, "spreadsheetId");
        String sheetName = str(config, "sheetName");
        String rowIndex = str(config, "rowIndex");
        if (spreadsheetId == null) return ActionResult.failure("'spreadsheetId' is required");
        if (rowIndex == null) return ActionResult.failure("'rowIndex' is required");

        Object valuesObj = config.get("values");
        if (!(valuesObj instanceof List<?> values) || values.isEmpty()) {
            return ActionResult.failure("'values' must be a non-empty list");
        }

        String range = (sheetName != null ? sheetName + "!" : "") + "A" + rowIndex;
        logger.info("[google-sheets] Updating row {} in spreadsheet '{}'", rowIndex, spreadsheetId);

        try {
            String url = SHEETS_API + "/" + spreadsheetId + "/values/" + range
                    + "?valueInputOption=USER_ENTERED";

            String response = restClient.put()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("values", List.of(values)))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-sheets");
            output.put("action", "update-row");
            output.put("spreadsheetId", spreadsheetId);
            output.put("rowIndex", rowIndex);
            output.put("response", response);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[google-sheets] Failed to update row: {}", e.getMessage());
            return ActionResult.failure("Google Sheets update-row failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }
}
