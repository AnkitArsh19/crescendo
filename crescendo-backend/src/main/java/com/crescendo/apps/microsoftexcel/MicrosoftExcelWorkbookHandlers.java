package com.crescendo.apps.microsoftexcel;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
// import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Grouped handler for Microsoft Excel Workbook operations.
 */
@Component
public class MicrosoftExcelWorkbookHandlers {

    private static final String GRAPH_API = MicrosoftExcelSupport.GRAPH_API;

    // ── createWorkbook ────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftexcel", actionKey = "createWorkbook")
    public ActionResult createWorkbook(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String fileName = MicrosoftExcelSupport.require(config, "fileName");
        if (fileName == null) return ActionResult.failure("'fileName' is required");
        if (!fileName.endsWith(".xlsx")) fileName = fileName + ".xlsx";

        String folderId = MicrosoftExcelSupport.opt(config, "folderId", null);

        try {
            String uri = folderId != null
                    ? GRAPH_API + "/me/drive/items/" + folderId + ":/" + fileName + ":/content"
                    : GRAPH_API + "/me/drive/root:/" + fileName + ":/content";

            String response = MicrosoftExcelSupport.clientBuilder(context).build().put()
                    .uri(uri)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new byte[0])
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("fileName", fileName);
            output.put("response", response);
            return ActionResult.success(output);
        } catch (Exception e) {
            return ActionResult.failure("Excel createWorkbook failed: " + e.getMessage());
        }
    }

    // ── createWorksheet ───────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftexcel", actionKey = "createWorksheet")
    public ActionResult createWorksheet(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String driveItemId = MicrosoftExcelSupport.require(config, "driveItemId");
        String sheetName = MicrosoftExcelSupport.require(config, "sheetName");
        if (driveItemId == null || sheetName == null) {
            return ActionResult.failure("'driveItemId' and 'sheetName' are required");
        }

        try {
            String response = MicrosoftExcelSupport.clientBuilder(context).build().post()
                    .uri(GRAPH_API + "/me/drive/items/" + driveItemId + "/workbook/worksheets")
                    .body(Map.of("name", sheetName))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            return ActionResult.success(output);
        } catch (Exception e) {
            return ActionResult.failure("Excel createWorksheet failed: " + e.getMessage());
        }
    }
}
