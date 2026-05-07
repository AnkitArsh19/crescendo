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
 * Creates a new worksheet (tab) in an existing Excel workbook.
 */
@ActionMapping(appKey = "microsoft-excel", actionKey = "create-worksheet")
public class MicrosoftExcelCreateWorksheetHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(MicrosoftExcelCreateWorksheetHandler.class);
    private static final String GRAPH_API = "https://graph.microsoft.com/v1.0";

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> creds = context.credentials();

        String accessToken = asString(creds.get("accessToken"));
        if (accessToken == null || accessToken.isBlank()) {
            return ActionResult.failure("Microsoft Excel requires 'accessToken' in connection credentials");
        }

        String driveItemId = asString(config.get("driveItemId"));
        String sheetName = asString(config.get("sheetName"));

        if (driveItemId == null || driveItemId.isBlank()) return ActionResult.failure("'driveItemId' is required");
        if (sheetName == null || sheetName.isBlank()) return ActionResult.failure("'sheetName' is required");

        try {
            String response = RestClient.create()
                    .post()
                    .uri(GRAPH_API + "/me/drive/items/" + driveItemId + "/workbook/worksheets")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("name", sheetName))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("provider", "microsoft-excel");
            output.put("action", "create-worksheet");
            output.put("response", response);
            logger.info("[microsoft-excel] Worksheet created successfully");
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[microsoft-excel] Create worksheet failed", e);
            return ActionResult.failure("Create worksheet failed: " + e.getMessage());
        }
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}
