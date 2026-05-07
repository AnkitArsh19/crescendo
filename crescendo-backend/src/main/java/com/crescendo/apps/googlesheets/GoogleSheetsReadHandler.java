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
import java.util.Map;

/**
 * Reads rows from a Google Sheet via Sheets API v4.
 *
 * <p>Connection credentials: {@code accessToken} (OAuth2)
 * <p>Config: {@code spreadsheetId}, {@code range}
 */
@ActionMapping(appKey = "google-sheets", actionKey = "read-rows")
public class GoogleSheetsReadHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsReadHandler.class);
    private static final String SHEETS_API = "https://sheets.googleapis.com/v4/spreadsheets";

    private final RestClient restClient;

    public GoogleSheetsReadHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = creds != null ? (String) creds.get("accessToken") : null;
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Google Sheets requires an 'accessToken' in connection credentials");
        }

        String spreadsheetId = getRequired(config, "spreadsheetId");
        String range = getRequired(config, "range");
        if (spreadsheetId == null) return ActionResult.failure("'spreadsheetId' is required");
        if (range == null) return ActionResult.failure("'range' is required");

        try {
            String url = SHEETS_API + "/" + spreadsheetId + "/values/" + range;

            String response = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-sheets");
            output.put("action", "read-rows");
            output.put("spreadsheetId", spreadsheetId);
            output.put("range", range);
            output.put("response", response);
            logger.info("[google-sheets] Rows read successfully");
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[google-sheets] Failed to read from {}: {}", spreadsheetId, e.getMessage());
            return ActionResult.failure("Google Sheets read failed: " + e.getMessage());
        }
    }

    private String getRequired(Map<String, Object> config, String key) {
        Object val = config.get(key);
        return val != null ? val.toString() : null;
    }
}
