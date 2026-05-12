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
 * Clears cells in a Google Sheet range without deleting the rows.
 */
@ActionMapping(appKey = "google-sheets", actionKey = "clear-cells")
public class GoogleSheetsClearCellsHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsClearCellsHandler.class);
    private static final String SHEETS_API = "https://sheets.googleapis.com/v4/spreadsheets";

    private final RestClient restClient;

    public GoogleSheetsClearCellsHandler() {
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
        String range = str(config, "range");
        if (spreadsheetId == null) return ActionResult.failure("'spreadsheetId' is required");
        if (range == null) return ActionResult.failure("'range' is required");

        String fullRange = (sheetName != null ? sheetName + "!" : "") + range;
        logger.info("[google-sheets] Clearing cells in range '{}' of spreadsheet '{}'", fullRange, spreadsheetId);

        try {
            String url = SHEETS_API + "/" + spreadsheetId + "/values/" + fullRange + ":clear";

            String response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of())
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-sheets");
            output.put("action", "clear-cells");
            output.put("spreadsheetId", spreadsheetId);
            output.put("clearedRange", fullRange);
            output.put("response", response);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[google-sheets] Clear-cells failed: {}", e.getMessage());
            return ActionResult.failure("Google Sheets clear-cells failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }
}
