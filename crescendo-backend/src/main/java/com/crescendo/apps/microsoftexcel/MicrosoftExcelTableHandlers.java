package com.crescendo.apps.microsoftexcel;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grouped handler for Microsoft Excel Table operations.
 */
@Component
public class MicrosoftExcelTableHandlers {

    private static final String GRAPH_API = MicrosoftExcelSupport.GRAPH_API;

    // ── appendRow ─────────────────────────────────────────────────────────────
    @ActionMapping(appKey = "microsoftexcel", actionKey = "appendRow")
    public ActionResult appendRow(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String driveItemId = MicrosoftExcelSupport.require(config, "driveItemId");
        String tableName = MicrosoftExcelSupport.require(config, "tableName");
        Object valuesObj = config.get("values");

        if (driveItemId == null || tableName == null) {
            return ActionResult.failure("'driveItemId' and 'tableName' are required");
        }
        if (!(valuesObj instanceof List<?> values) || values.isEmpty()) {
            return ActionResult.failure("'values' must be a non-empty array");
        }

        try {
            String uri = GRAPH_API + "/me/drive/items/" + driveItemId + "/workbook/tables/" + tableName + "/rows/add";
            String response = MicrosoftExcelSupport.clientBuilder(context).build().post()
                    .uri(uri)
                    .body(Map.of("values", List.of(values)))
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            return ActionResult.success(output);
        } catch (Exception e) {
            return ActionResult.failure("Excel appendRow failed: " + e.getMessage());
        }
    }
}
