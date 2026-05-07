package com.crescendo.apps.googlesheets;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Fetches Google Sheets resources (spreadsheets, worksheets, columns) from
 * the user's connected Google account.
 * <p>
 * Resource types and their cascade dependencies:
 * <ol>
 *   <li>{@code spreadsheets} — lists all spreadsheets (no parent params)</li>
 *   <li>{@code worksheets} — requires {@code spreadsheetId}</li>
 *   <li>{@code columns} — requires {@code spreadsheetId} + {@code sheetName}</li>
 * </ol>
 */
@Component
public class GoogleSheetsResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsResourceProvider.class);

    private static final String DRIVE_API = "https://www.googleapis.com/drive/v3/files";
    private static final String SHEETS_API = "https://sheets.googleapis.com/v4/spreadsheets";

    private final RestClient restClient;

    public GoogleSheetsResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "google-sheets";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("spreadsheets", "worksheets", "columns");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String accessToken = extractToken(credentials);

        return switch (resourceType) {
            case "spreadsheets" -> listSpreadsheets(accessToken);
            case "worksheets" -> listWorksheets(accessToken, requireParam(params, "spreadsheetId"));
            case "columns" -> listColumns(accessToken,
                    requireParam(params, "spreadsheetId"),
                    requireParam(params, "sheetName"));
            default -> List.of();
        };
    }

    // ─── Spreadsheets ────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listSpreadsheets(String accessToken) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(DRIVE_API + "?q=mimeType='application/vnd.google-apps.spreadsheet'+and+trashed=false"
                         + "&fields=files(id,name)&orderBy=modifiedTime+desc&pageSize=50")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("files")) return List.of();

            List<Map<String, String>> files = (List<Map<String, String>>) response.get("files");
            return files.stream()
                    .map(f -> new ResourceOption(f.get("id"), f.get("name"), "ID: " + f.get("id")))
                    .toList();

        } catch (Exception e) {
            logger.error("[google-sheets] Failed to list spreadsheets: {}", e.getMessage());
            return List.of();
        }
    }

    // ─── Worksheets (sheets within a spreadsheet) ────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listWorksheets(String accessToken, String spreadsheetId) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(SHEETS_API + "/" + spreadsheetId + "?fields=sheets(properties(sheetId,title,index))")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("sheets")) return List.of();

            List<Map<String, Object>> sheets = (List<Map<String, Object>>) response.get("sheets");
            return sheets.stream()
                    .map(s -> {
                        Map<String, Object> props = (Map<String, Object>) s.get("properties");
                        String title = String.valueOf(props.get("title"));
                        String sheetId = String.valueOf(props.get("sheetId"));
                        return new ResourceOption(title, title, "ID: " + sheetId);
                    })
                    .toList();

        } catch (Exception e) {
            logger.error("[google-sheets] Failed to list worksheets for {}: {}", spreadsheetId, e.getMessage());
            return List.of();
        }
    }

    // ─── Columns (header row) ────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listColumns(String accessToken, String spreadsheetId, String sheetName) {
        try {
            String range = sheetName + "!1:1";
            Map<String, Object> response = restClient.get()
                    .uri(SHEETS_API + "/" + spreadsheetId + "/values/" + range)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("values")) return List.of();

            List<List<String>> values = (List<List<String>>) response.get("values");
            if (values.isEmpty()) return List.of();

            List<String> headers = values.get(0);
            List<ResourceOption> options = new ArrayList<>();
            for (int i = 0; i < headers.size(); i++) {
                String header = headers.get(i);
                String colLetter = columnIndexToLetter(i);
                options.add(new ResourceOption(colLetter, header, "Column " + colLetter));
            }
            return options;

        } catch (Exception e) {
            logger.error("[google-sheets] Failed to list columns for {}!{}: {}",
                    spreadsheetId, sheetName, e.getMessage());
            return List.of();
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private String extractToken(Map<String, Object> credentials) {
        Object token = credentials.get("accessToken");
        if (token == null || token.toString().isBlank()) {
            throw new IllegalArgumentException("Google Sheets connection is missing 'accessToken'");
        }
        return token.toString();
    }

    private String requireParam(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required parameter '" + key + "' is missing");
        }
        return value;
    }

    private static String columnIndexToLetter(int index) {
        StringBuilder sb = new StringBuilder();
        int col = index;
        while (col >= 0) {
            sb.insert(0, (char) ('A' + col % 26));
            col = col / 26 - 1;
        }
        return sb.toString();
    }
}
