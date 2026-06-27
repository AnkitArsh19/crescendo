package com.crescendo.apps.microsoftexcel;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.core.ParameterizedTypeReference;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
// import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grouped handler for Microsoft Excel Worksheet operations.
 */
@Component
public class MicrosoftExcelWorksheetHandlers {

// private static final Logger logger = LoggerFactory.getLogger(MicrosoftExcelWorksheetHandlers.class);
    private static final String GRAPH_API = MicrosoftExcelSupport.GRAPH_API;

    // ── getAll ────────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftexcel", actionKey = "getAllWorksheets")
    @SuppressWarnings("unchecked")
    public ActionResult getAll(ActionContext context) {
        String driveItemId = MicrosoftExcelSupport.require(context.configuration(), "driveItemId");
        if (driveItemId == null) return ActionResult.failure("'driveItemId' is required");

        try {
            Map<String, Object> response = MicrosoftExcelSupport.clientBuilder(context).build().get()
                    .uri(GRAPH_API + "/me/drive/items/" + driveItemId + "/workbook/worksheets")
                    .retrieve()
                    .body(Map.class);
            return ActionResult.success(response);
        } catch (Exception e) {
            return ActionResult.failure("Excel getAllWorksheets failed: " + e.getMessage());
        }
    }

    // ── readRows ──────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftexcel", actionKey = "readRows")
    @SuppressWarnings("unchecked")
    public ActionResult readRows(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String workbookId = MicrosoftExcelSupport.require(config, "driveItemId");
        String worksheetId = MicrosoftExcelSupport.require(config, "worksheetId");
        if (workbookId == null || worksheetId == null) return ActionResult.failure("'driveItemId' and 'worksheetId' are required");

        String range = MicrosoftExcelSupport.opt(config, "range", null);
        int keyRow = MicrosoftExcelSupport.parseIntOpt(config, "keyRow", 0);
        int dataStartRow = MicrosoftExcelSupport.parseIntOpt(config, "dataStartRow", 1);

        try {
            String uri = GRAPH_API + "/me/drive/items/" + workbookId + "/workbook/worksheets/" + worksheetId;
            if (range != null && !range.isBlank()) uri += "/range(address='" + range + "')";
            else uri += "/usedRange";

            Map<String, Object> response = MicrosoftExcelSupport.clientBuilder(context).build().get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("text")) {
                return ActionResult.failure("Invalid response from Excel API (missing 'text' matrix)");
            }

            List<List<Object>> textMatrix = (List<List<Object>>) response.get("text");
            List<Map<String, Object>> mappedRows = new ArrayList<>();

            if (textMatrix.size() > keyRow && textMatrix.size() > dataStartRow) {
                List<Object> headers = textMatrix.get(keyRow);
                for (int i = dataStartRow; i < textMatrix.size(); i++) {
                    List<Object> row = textMatrix.get(i);
                    boolean isEmpty = true;
                    for (Object val : row) {
                        if (val != null && !val.toString().isBlank()) {
                            isEmpty = false;
                            break;
                        }
                    }
                    if (isEmpty) continue;

                    Map<String, Object> mappedRow = new HashMap<>();
                    for (int j = 0; j < headers.size() && j < row.size(); j++) {
                        String key = headers.get(j) != null ? headers.get(j).toString().trim() : "Column" + j;
                        if (key.isBlank()) key = "Column" + j;
                        mappedRow.put(key, row.get(j));
                    }
                    mappedRow.put("_rowNumber", i + 1);
                    mappedRows.add(mappedRow);
                }
            }

            return ActionResult.success(Map.of("rowCount", mappedRows.size(), "rows", mappedRows));
        } catch (Exception e) {
            return ActionResult.failure("Excel readRows failed: " + e.getMessage());
        }
    }

    // ── findRow ───────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftexcel", actionKey = "findRow")
    @SuppressWarnings("unchecked")
    public ActionResult findRow(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String driveItemId = MicrosoftExcelSupport.require(config, "driveItemId");
        String worksheetId = MicrosoftExcelSupport.require(config, "worksheetId");
        String searchValue = MicrosoftExcelSupport.require(config, "searchValue");
        if (driveItemId == null || worksheetId == null || searchValue == null) {
            return ActionResult.failure("'driveItemId', 'worksheetId', and 'searchValue' are required");
        }
        String searchColumn = MicrosoftExcelSupport.opt(config, "searchColumn", null);

        try {
            String uri = GRAPH_API + "/me/drive/items/" + driveItemId + "/workbook/worksheets/" + worksheetId + "/usedRange";
            Map<String, Object> rangeData = MicrosoftExcelSupport.clientBuilder(context).build().get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            List<List<Object>> rows = (List<List<Object>>) rangeData.get("values");

            if (rows == null || rows.isEmpty()) return ActionResult.success(Map.of("found", false, "message", "Worksheet is empty"));

            int colIdx = 0;
            if (searchColumn != null && !searchColumn.isBlank()) {
                try {
                    colIdx = Integer.parseInt(searchColumn);
                } catch (NumberFormatException e) {
                    List<Object> headers = rows.get(0);
                    for (int i = 0; i < headers.size(); i++) {
                        if (searchColumn.equalsIgnoreCase(String.valueOf(headers.get(i)))) {
                            colIdx = i;
                            break;
                        }
                    }
                }
            }

            for (int r = 0; r < rows.size(); r++) {
                List<Object> row = rows.get(r);
                if (colIdx < row.size() && searchValue.equals(String.valueOf(row.get(colIdx)))) {
                    Map<String, Object> output = new HashMap<>();
                    output.put("found", true);
                    output.put("rowIndex", r + 1);
                    output.put("rowData", row);
                    return ActionResult.success(output);
                }
            }
            return ActionResult.success(Map.of("found", false, "message", "No row found with '" + searchValue + "' in column " + colIdx));
        } catch (Exception e) {
            return ActionResult.failure("Excel findRow failed: " + e.getMessage());
        }
    }

    // ── updateRow ─────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftexcel", actionKey = "updateRow")
    public ActionResult updateRow(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String driveItemId = MicrosoftExcelSupport.require(config, "driveItemId");
        String worksheetId = MicrosoftExcelSupport.require(config, "worksheetId");
        String rowIndexStr = MicrosoftExcelSupport.require(config, "rowIndex");
        if (driveItemId == null || worksheetId == null || rowIndexStr == null) {
            return ActionResult.failure("'driveItemId', 'worksheetId', and 'rowIndex' are required");
        }
        Object valuesObj = config.get("values");
        if (!(valuesObj instanceof List<?> values) || values.isEmpty()) {
            return ActionResult.failure("'values' must be a non-empty array");
        }

        try {
            int rowIndex = Integer.parseInt(rowIndexStr);
            String range = "A" + rowIndex + ":Z" + rowIndex;
            String uri = GRAPH_API + "/me/drive/items/" + driveItemId + "/workbook/worksheets/" + worksheetId + "/range(address='" + range + "')";

            String response = MicrosoftExcelSupport.clientBuilder(context).build().patch()
                    .uri(uri)
                    .body(Map.of("values", List.of(values)))
                    .retrieve()
                    .body(String.class);
            return ActionResult.success(Map.of("rowIndex", rowIndex, "response", response));
        } catch (NumberFormatException e) {
            return ActionResult.failure("'rowIndex' must be a number");
        } catch (Exception e) {
            return ActionResult.failure("Excel updateRow failed: " + e.getMessage());
        }
    }

    // ── deleteRow ─────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftexcel", actionKey = "deleteRow")
    public ActionResult deleteRow(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String driveItemId = MicrosoftExcelSupport.require(config, "driveItemId");
        String worksheetId = MicrosoftExcelSupport.require(config, "worksheetId");
        String rowIndexStr = MicrosoftExcelSupport.require(config, "rowIndex");
        if (driveItemId == null || worksheetId == null || rowIndexStr == null) {
            return ActionResult.failure("'driveItemId', 'worksheetId', and 'rowIndex' are required");
        }

        try {
            int rowIndex = Integer.parseInt(rowIndexStr);
            String range = "A" + rowIndex + ":ZZ" + rowIndex;
            String uri = GRAPH_API + "/me/drive/items/" + driveItemId + "/workbook/worksheets/" + worksheetId + "/range(address='" + range + "')/delete";

            MicrosoftExcelSupport.clientBuilder(context).build().post()
                    .uri(uri)
                    .body(Map.of("shift", "Up"))
                    .retrieve()
                    .toBodilessEntity();
            return ActionResult.success(Map.of("rowIndex", rowIndex, "status", "deleted"));
        } catch (NumberFormatException e) {
            return ActionResult.failure("'rowIndex' must be a number");
        } catch (Exception e) {
            return ActionResult.failure("Excel deleteRow failed: " + e.getMessage());
        }
    }
}
