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
 * Creates a new Google Sheets spreadsheet.
 */
@ActionMapping(appKey = "google-sheets", actionKey = "create-spreadsheet")
public class GoogleSheetsCreateSpreadsheetHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsCreateSpreadsheetHandler.class);
    private static final String SHEETS_API = "https://sheets.googleapis.com/v4/spreadsheets";

    private final RestClient restClient;

    public GoogleSheetsCreateSpreadsheetHandler() {
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

        String title = str(config, "title");
        if (title == null) return ActionResult.failure("'title' is required");

        logger.info("[google-sheets] Creating spreadsheet: title='{}'", title);

        try {
            Map<String, Object> body = Map.of(
                "properties", Map.of("title", title)
            );

            Map<String, Object> response = restClient.post()
                    .uri(SHEETS_API)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-sheets");
            output.put("action", "create-spreadsheet");
            output.put("spreadsheetId", response.get("spreadsheetId"));
            output.put("spreadsheetUrl", response.get("spreadsheetUrl"));
            output.put("title", title);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[google-sheets] Create spreadsheet failed: {}", e.getMessage());
            return ActionResult.failure("Google Sheets create-spreadsheet failed: " + e.getMessage());
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }
}
