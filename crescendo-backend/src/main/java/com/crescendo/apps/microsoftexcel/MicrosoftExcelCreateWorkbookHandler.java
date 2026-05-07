package com.crescendo.apps.microsoftexcel;

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
 * Creates a new Excel workbook in OneDrive.
 */
@ActionMapping(appKey = "microsoft-excel", actionKey = "create-workbook")
public class MicrosoftExcelCreateWorkbookHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(MicrosoftExcelCreateWorkbookHandler.class);
    private static final String GRAPH_API = "https://graph.microsoft.com/v1.0";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = asString(creds.get("accessToken"));
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Microsoft Excel requires 'accessToken'");
        }

        String fileName = asString(config.get("fileName"));
        String folderId = asString(config.get("folderId"));

        if (fileName == null || fileName.isBlank()) return ActionResult.failure("'fileName' is required");
        if (!fileName.endsWith(".xlsx")) {
            fileName = fileName + ".xlsx";
        }

        try {
            // Upload an empty Excel file to OneDrive
            // PUT /me/drive/items/{folderId}:/{fileName}:/content  OR  /me/drive/root:/{fileName}:/content
            String uri;
            if (folderId != null && !folderId.isBlank()) {
                uri = GRAPH_API + "/me/drive/items/" + folderId + ":/" + fileName + ":/content";
            } else {
                uri = GRAPH_API + "/me/drive/root:/" + fileName + ":/content";
            }

            // Create a minimal empty xlsx by using the Graph API workbook session approach
            // Alternatively, we just create an empty file placeholder
            String response = RestClient.create()
                    .put()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new byte[0]) // Empty content — Graph will create a valid Excel file
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "microsoft-excel");
            output.put("action", "create-workbook");
            output.put("fileName", fileName);
            output.put("response", response);
            logger.info("[microsoft-excel] Workbook created successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[microsoft-excel] Create workbook failed", e);
            return ActionResult.failure("Create workbook failed: " + e.getMessage());
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}
