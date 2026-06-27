package com.crescendo.apps.googlesheets;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Grouped handler for Google Sheets spreadsheet-level operations.
 *
 * <p>Operations (mirrors n8n {@code SpreadsheetDescription.ts}):
 * <ul>
 *   <li>{@code create} — spreadsheets.create (POST /v4/spreadsheets)</li>
 *   <li>{@code get}    — spreadsheets.get    (GET  /v4/spreadsheets/{id})</li>
 * </ul>
 *
 * <p>Credentials: {@code accessToken} (OAuth2, sheets scope)
 */
@Component
public class GoogleSheetsSpreadsheetHandlers {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsSpreadsheetHandlers.class);

    private static final String BASE = "https://sheets.googleapis.com/v4/spreadsheets";

    private final RestClient restClient;

    public GoogleSheetsSpreadsheetHandlers() {
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ── create ────────────────────────────────────────────────────────────────

    /**
     * Create a new Google Sheets spreadsheet.
     * Config: title (required)
     */
    @ActionMapping(appKey = "googlesheets", actionKey = "create")
    @SuppressWarnings("unchecked")
    public ActionResult create(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String title = require(config, "title");
        if (title == null) return ActionResult.failure("'title' is required");

        logger.info("[googlesheets] create: title='{}'", title);

        try {
            Map<String, Object> response = restClient.post()
                    .uri(BASE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(Map.of("properties", Map.of("title", title)))
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googlesheets] create failed: {}", e.getMessage());
            return ActionResult.failure("Google Sheets create failed: " + e.getMessage());
        }
    }

    // ── get ───────────────────────────────────────────────────────────────────

    /**
     * Get a spreadsheet's metadata.
     * Config: spreadsheetId (required)
     */
    @ActionMapping(appKey = "googlesheets", actionKey = "getSpreadsheet")
    @SuppressWarnings("unchecked")
    public ActionResult get(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String spreadsheetId = require(config, "spreadsheetId");
        if (spreadsheetId == null) return ActionResult.failure("'spreadsheetId' is required");

        logger.info("[googlesheets] getSpreadsheet: spreadsheetId='{}'", spreadsheetId);

        try {
            Map<String, Object> response = restClient.get()
                    .uri(BASE + "/" + spreadsheetId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googlesheets] getSpreadsheet failed: {}", e.getMessage());
            return ActionResult.failure("Google Sheets getSpreadsheet failed: " + e.getMessage());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String resolveToken(ActionContext context) {
        Map<String, Object> creds = context.credentials();
        if (creds == null) return null;
        Object token = creds.get("accessToken");
        return token != null && !token.toString().isBlank() ? token.toString() : null;
    }

    private ActionResult missingToken() {
        return ActionResult.failure("Google Sheets requires an OAuth2 accessToken in connection credentials");
    }

    private String require(Map<String, Object> config, String key) {
        Object v = config.get(key);
        return (v != null && !v.toString().isBlank()) ? v.toString() : null;
    }
}
