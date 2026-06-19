package com.crescendo.apps.webhook;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "crescendo-webhook", actionKey = "respond")
public class WebhookRespondHandler implements ActionHandler {
    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> out = new HashMap<>(context.inputData() != null ? context.inputData() : Map.of());
        out.put("_webhookResponse", Map.of(
                "status", intValue(context.configuration().get("status"), 200),
                "body", context.configuration().getOrDefault("body", Map.of("ok", true)),
                "headers", context.configuration().getOrDefault("headers", Map.of())
        ));
        return ActionResult.success(out);
    }

    private int intValue(Object value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
