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
 * Appends a row to a Google Sheet via Sheets API v4.
 *
 * <p>Connection credentials: {@code accessToken} (OAuth2)
 * <p>Config: {@code spreadsheetId}, {@code range}, {@code values}
 */
@ActionMapping(appKey = "google-sheets", actionKey = "append-row")
public class GoogleSheetsAppendHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsAppendHandler.class);
    private static final String SHEETS_API = "https://sheets.googleapis.com/v4/spreadsheets";

    private final RestClient restClient;

    public GoogleSheetsAppendHandler() {
        this.restClient = RestClient.create();
    }

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = creds != null ? (String) creds.get("accessToken") : null;
        if (accessToken == null || accessToken.isBlank()) {
            logger.warn("[google-sheets] append-row: missing accessToken");
            return ActionResult.failure("Google Sheets requires an 'accessToken' in connection credentials");
        }

        String spreadsheetId = getRequired(config, "spreadsheetId");
        String range = getRequired(config, "range");
        if (spreadsheetId == null) return ActionResult.failure("'spreadsheetId' is required");
        if (range == null) return ActionResult.failure("'range' is required");

        Object valuesObj = config.get("values");
        if (!(valuesObj instanceof List<?> values) || values.isEmpty()) {
            return ActionResult.failure("'values' must be a non-empty list");
        }

        logger.info("[google-sheets] Appending row to spreadsheet='{}', range='{}'", spreadsheetId, range);

        try {
            String url = SHEETS_API + "/" + spreadsheetId + "/values/" + range
                    + ":append?valueInputOption=USER_ENTERED&insertDataOption=INSERT_ROWS";

            String response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("values", List.of(values)))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "google-sheets");
            output.put("action", "append-row");
            output.put("spreadsheetId", spreadsheetId);
            output.put("range", range);
            output.put("response", response);
            logger.info("[google-sheets] Row appended successfully to spreadsheet='{}'", spreadsheetId);
            return ActionResult.success(output);

        } catch (Exception e) {
            logger.error("[google-sheets] Failed to append row to {}: {}", spreadsheetId, e.getMessage());
            return ActionResult.failure("Google Sheets append failed: " + e.getMessage());
        }
    }

    private String getRequired(Map<String, Object> config, String key) {
        Object val = config.get(key);
        return val != null ? val.toString() : null;
    }
}
