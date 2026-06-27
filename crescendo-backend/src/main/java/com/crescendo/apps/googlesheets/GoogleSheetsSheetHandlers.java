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

import java.util.List;
import java.util.Map;

/**
 * Grouped handler for Google Sheets sheet-level (row/data) operations.
 *
 * <p>Operations (mirrors n8n {@code SheetDescription.ts}):
 * <ul>
 *   <li>{@code appendRow}  — spreadsheets.values.append</li>
 *   <li>{@code clearCells} — spreadsheets.values.clear</li>
 *   <li>{@code deleteRow}  — spreadsheets.batchUpdate (deleteDimension)</li>
 *   <li>{@code findRow}    — spreadsheets.values.get + client-side filter</li>
 *   <li>{@code readRows}   — spreadsheets.values.get</li>
 *   <li>{@code updateRow}  — spreadsheets.values.update</li>
 * </ul>
 *
 * <p>Credentials: {@code accessToken} (OAuth2, sheets scope)
 */
@Component
public class GoogleSheetsSheetHandlers {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsSheetHandlers.class);

    private static final String BASE = "https://sheets.googleapis.com/v4/spreadsheets";

    private final RestClient restClient;

    public GoogleSheetsSheetHandlers() {
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ── appendRow ─────────────────────────────────────────────────────────────

    /**
     * Append a row to a sheet range.
     * Config: spreadsheetId (required), range (required, e.g. "Sheet1!A1"),
     *         values (required, List of cell values)
     */
    @ActionMapping(appKey = "googlesheets", actionKey = "appendRow")
    @SuppressWarnings("unchecked")
    public ActionResult appendRow(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String spreadsheetId = require(config, "spreadsheetId");
        if (spreadsheetId == null) return ActionResult.failure("'spreadsheetId' is required");
        String range = require(config, "range");
        if (range == null) return ActionResult.failure("'range' is required");
        Object valuesObj = config.get("values");
        if (!(valuesObj instanceof List<?> values) || values.isEmpty())
            return ActionResult.failure("'values' must be a non-empty list");

        logger.info("[googlesheets] appendRow: spreadsheetId='{}', range='{}'", spreadsheetId, range);

        try {
            String url = BASE + "/" + spreadsheetId + "/values/" + range
                    + ":append?valueInputOption=USER_ENTERED&insertDataOption=INSERT_ROWS";

            Map<String, Object> response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(Map.of("values", List.of(values)))
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googlesheets] appendRow failed: {}", e.getMessage());
            return ActionResult.failure("Google Sheets appendRow failed: " + e.getMessage());
        }
    }

    // ── clearCells ────────────────────────────────────────────────────────────

    /**
     * Clear all values in a range.
     * Config: spreadsheetId (required), range (required)
     */
    @ActionMapping(appKey = "googlesheets", actionKey = "clearCells")
    @SuppressWarnings("unchecked")
    public ActionResult clearCells(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String spreadsheetId = require(config, "spreadsheetId");
        if (spreadsheetId == null) return ActionResult.failure("'spreadsheetId' is required");
        String range = require(config, "range");
        if (range == null) return ActionResult.failure("'range' is required");

        logger.info("[googlesheets] clearCells: spreadsheetId='{}', range='{}'", spreadsheetId, range);

        try {
            Map<String, Object> response = restClient.post()
                    .uri(BASE + "/" + spreadsheetId + "/values/" + range + ":clear")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(Map.of())
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googlesheets] clearCells failed: {}", e.getMessage());
            return ActionResult.failure("Google Sheets clearCells failed: " + e.getMessage());
        }
    }

    // ── deleteRow ─────────────────────────────────────────────────────────────

    /**
     * Delete a row by index (0-based internally, 1-based from config).
     * Config: spreadsheetId (required), sheetId (int, required), rowIndex (int, 1-based)
     */
    @ActionMapping(appKey = "googlesheets", actionKey = "deleteRow")
    @SuppressWarnings("unchecked")
    public ActionResult deleteRow(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String spreadsheetId = require(config, "spreadsheetId");
        if (spreadsheetId == null) return ActionResult.failure("'spreadsheetId' is required");
        int sheetId = parseIntOpt(config, "sheetId", 0);
        int rowIndex = parseIntOpt(config, "rowIndex", 1) - 1; // convert 1-based to 0-based

        logger.info("[googlesheets] deleteRow: spreadsheetId='{}', rowIndex={}", spreadsheetId, rowIndex + 1);

        try {
            Map<String, Object> body = Map.of(
                    "requests", List.of(Map.of(
                            "deleteDimension", Map.of(
                                    "range", Map.of(
                                            "sheetId", sheetId,
                                            "dimension", "ROWS",
                                            "startIndex", rowIndex,
                                            "endIndex", rowIndex + 1
                                    )
                            )
                    ))
            );

            Map<String, Object> response = restClient.post()
                    .uri(BASE + "/" + spreadsheetId + ":batchUpdate")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googlesheets] deleteRow failed: {}", e.getMessage());
            return ActionResult.failure("Google Sheets deleteRow failed: " + e.getMessage());
        }
    }

    // ── findRow ───────────────────────────────────────────────────────────────

    /**
     * Find rows matching a column value.
     * Config: spreadsheetId (required), range (required),
     *         keyColumn (int, 0-based, default 0), keyValue (required)
     */
    @ActionMapping(appKey = "googlesheets", actionKey = "findRow")
    @SuppressWarnings("unchecked")
    public ActionResult findRow(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String spreadsheetId = require(config, "spreadsheetId");
        if (spreadsheetId == null) return ActionResult.failure("'spreadsheetId' is required");
        String range = require(config, "range");
        if (range == null) return ActionResult.failure("'range' is required");
        String keyValue = require(config, "keyValue");
        if (keyValue == null) return ActionResult.failure("'keyValue' is required");
        int keyColumn = parseIntOpt(config, "keyColumn", 0);

        logger.info("[googlesheets] findRow: spreadsheetId='{}', range='{}', keyValue='{}'", spreadsheetId, range, keyValue);

        try {
            Map<String, Object> data = restClient.get()
                    .uri(BASE + "/" + spreadsheetId + "/values/" + range)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            java.util.List<java.util.List<Object>> rows = data != null ? (java.util.List<java.util.List<Object>>) data.get("values") : java.util.List.of();
            java.util.List<java.util.List<Object>> matched = new java.util.ArrayList<>();
            for (java.util.List<Object> row : rows) {
                if (row.size() > keyColumn && keyValue.equals(row.get(keyColumn).toString())) {
                    matched.add(row);
                }
            }

            return ActionResult.success(Map.of("values", matched, "count", matched.size()));
        } catch (Exception e) {
            logger.error("[googlesheets] findRow failed: {}", e.getMessage());
            return ActionResult.failure("Google Sheets findRow failed: " + e.getMessage());
        }
    }

    // ── readRows ──────────────────────────────────────────────────────────────

    /**
     * Read rows from a sheet range.
     * Config: spreadsheetId (required), range (required)
     */
    @ActionMapping(appKey = "googlesheets", actionKey = "readRows")
    @SuppressWarnings("unchecked")
    public ActionResult readRows(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String spreadsheetId = require(config, "spreadsheetId");
        if (spreadsheetId == null) return ActionResult.failure("'spreadsheetId' is required");
        String range = require(config, "range");
        if (range == null) return ActionResult.failure("'range' is required");

        logger.info("[googlesheets] readRows: spreadsheetId='{}', range='{}'", spreadsheetId, range);

        try {
            Map<String, Object> response = restClient.get()
                    .uri(BASE + "/" + spreadsheetId + "/values/" + range)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googlesheets] readRows failed: {}", e.getMessage());
            return ActionResult.failure("Google Sheets readRows failed: " + e.getMessage());
        }
    }

    // ── updateRow ─────────────────────────────────────────────────────────────

    /**
     * Update a row at a specific range.
     * Config: spreadsheetId (required), range (required, e.g. "Sheet1!A2"),
     *         values (required, List of cell values)
     */
    @ActionMapping(appKey = "googlesheets", actionKey = "updateRow")
    @SuppressWarnings("unchecked")
    public ActionResult updateRow(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String accessToken = resolveToken(context);
        if (accessToken == null) return missingToken();

        String spreadsheetId = require(config, "spreadsheetId");
        if (spreadsheetId == null) return ActionResult.failure("'spreadsheetId' is required");
        String range = require(config, "range");
        if (range == null) return ActionResult.failure("'range' is required");
        Object valuesObj = config.get("values");
        if (!(valuesObj instanceof List<?> values) || values.isEmpty())
            return ActionResult.failure("'values' must be a non-empty list");

        logger.info("[googlesheets] updateRow: spreadsheetId='{}', range='{}'", spreadsheetId, range);

        try {
            String url = BASE + "/" + spreadsheetId + "/values/" + range + "?valueInputOption=USER_ENTERED";

            Map<String, Object> response = restClient.put()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(Map.of("values", List.of(values)))
                    .retrieve()
                    .body(Map.class);

            return ActionResult.success(response);
        } catch (Exception e) {
            logger.error("[googlesheets] updateRow failed: {}", e.getMessage());
            return ActionResult.failure("Google Sheets updateRow failed: " + e.getMessage());
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

    private int parseIntOpt(Map<String, Object> config, String key, int defaultVal) {
        Object v = config.get(key);
        if (v == null) return defaultVal;
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return defaultVal; }
    }
}
