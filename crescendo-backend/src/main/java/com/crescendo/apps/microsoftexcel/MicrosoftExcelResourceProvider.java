package com.crescendo.apps.microsoftexcel;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Lists workbooks, worksheets, columns, and folders from the user's OneDrive via Microsoft Graph API.
 * Supports: workbooks, worksheets (depends on driveItemId), columns (depends on driveItemId + worksheetId), folders
 */
@Component
public class MicrosoftExcelResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(MicrosoftExcelResourceProvider.class);
    private static final String GRAPH_API = "https://graph.microsoft.com/v1.0";

    @Override
    public String appKey() {
        return "microsoft-excel";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("workbooks", "worksheets", "columns", "folders");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String accessToken = credentials.get("accessToken").toString();

        return switch (resourceType) {
            case "workbooks" -> listWorkbooks(accessToken);
            case "worksheets" -> listWorksheets(accessToken, params.get("driveItemId"));
            case "columns" -> listColumns(accessToken, params.get("driveItemId"), params.get("worksheetId"));
            case "folders" -> listFolders(accessToken);
            default -> List.of();
        };
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listWorkbooks(String accessToken) {
        try {
            // Search OneDrive for .xlsx files
            Map<String, Object> response = restClient(accessToken)
                    .get()
                    .uri(GRAPH_API + "/me/drive/root/search(q='.xlsx')?$select=id,name,lastModifiedDateTime,parentReference&$top=100")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("value");
            if (items == null) return List.of();

            return items.stream()
                    .map(item -> {
                        String folder = "";
                        if (item.get("parentReference") instanceof Map parent) {
                            String path = parent.get("path") != null ? parent.get("path").toString() : "";
                            // Path format: /drive/root:/Documents — extract after "root:"
                            int idx = path.indexOf("root:");
                            folder = idx >= 0 ? path.substring(idx + 5) : "";
                            if (folder.startsWith("/")) folder = folder.substring(1);
                            if (folder.isEmpty()) folder = "Root";
                        }
                        return new ResourceOption(
                                item.get("id").toString(),
                                item.get("name").toString(),
                                "Folder: " + folder
                        );
                    })
                    .toList();
        } catch (Exception e) {
            logger.error("[microsoft-excel] Failed to list workbooks: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listWorksheets(String accessToken, String driveItemId) {
        if (driveItemId == null || driveItemId.isBlank()) return List.of();

        try {
            Map<String, Object> response = restClient(accessToken)
                    .get()
                    .uri(GRAPH_API + "/me/drive/items/{driveItemId}/workbook/worksheets", driveItemId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> sheets = (List<Map<String, Object>>) response.get("value");
            if (sheets == null) return List.of();

            return sheets.stream()
                    .map(sheet -> new ResourceOption(
                            sheet.get("id").toString(),
                            sheet.get("name").toString(),
                            sheet.get("position") != null ? "Position: " + sheet.get("position") : null
                    ))
                    .toList();
        } catch (Exception e) {
            logger.error("[microsoft-excel] Failed to list worksheets for {}: {}", driveItemId, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listColumns(String accessToken, String driveItemId, String worksheetId) {
        if (driveItemId == null || driveItemId.isBlank() || worksheetId == null || worksheetId.isBlank()) {
            return List.of();
        }

        try {
            // Get the used range to extract column headers from the first row
            Map<String, Object> response = restClient(accessToken)
                    .get()
                    .uri(GRAPH_API + "/me/drive/items/{driveItemId}/workbook/worksheets/{worksheetId}/usedRange?$select=values,columnCount,address",
                            driveItemId, worksheetId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<List<Object>> values = (List<List<Object>>) response.get("values");
            if (values == null || values.isEmpty()) return List.of();

            // First row = headers
            List<Object> headers = values.get(0);
            List<ResourceOption> options = new ArrayList<>();
            for (int i = 0; i < headers.size(); i++) {
                String colLetter = columnIndexToLetter(i);
                String headerName = headers.get(i) != null ? headers.get(i).toString().trim() : "";
                String label = headerName.isEmpty() ? "Column " + colLetter : headerName;
                String desc = "Column " + colLetter + (headerName.isEmpty() ? " (no header)" : "");
                options.add(new ResourceOption(colLetter, label, desc));
            }
            return options;
        } catch (Exception e) {
            logger.error("[microsoft-excel] Failed to list columns for {} / {}: {}", driveItemId, worksheetId, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listFolders(String accessToken) {
        try {
            // List top-level folders from OneDrive root
            Map<String, Object> response = restClient(accessToken)
                    .get()
                    .uri(GRAPH_API + "/me/drive/root/children?$select=id,name,folder&$filter=folder ne null&$top=100")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("value");
            if (items == null) return List.of();

            List<ResourceOption> options = new ArrayList<>();
            options.add(new ResourceOption("root", "Root (My Drive)", "Top-level OneDrive folder"));

            items.stream()
                    .filter(item -> item.get("folder") != null)
                    .forEach(item -> {
                        Map<String, Object> folder = (Map<String, Object>) item.get("folder");
                        int childCount = folder.get("childCount") != null ? ((Number) folder.get("childCount")).intValue() : 0;
                        options.add(new ResourceOption(
                                item.get("id").toString(),
                                item.get("name").toString(),
                                childCount + " items"
                        ));
                    });

            return options;
        } catch (Exception e) {
            logger.error("[microsoft-excel] Failed to list folders: {}", e.getMessage());
            return List.of();
        }
    }

    /** Converts 0-based column index to Excel-style letter (0=A, 1=B, ..., 25=Z, 26=AA) */
    private static String columnIndexToLetter(int index) {
        StringBuilder sb = new StringBuilder();
        int n = index;
        do {
            sb.insert(0, (char) ('A' + n % 26));
            n = n / 26 - 1;
        } while (n >= 0);
        return sb.toString();
    }

    private RestClient restClient(String accessToken) {
        return RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
