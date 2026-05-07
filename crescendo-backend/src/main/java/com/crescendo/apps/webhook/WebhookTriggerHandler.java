package com.crescendo.apps.webhook;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Pass-through handler for webhook trigger steps.
 * Returns the incoming trigger payload as the step's output.
 */
@ActionMapping(appKey = "crescendo-webhook", actionKey = "receive")
public class WebhookTriggerHandler implements ActionHandler {

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> output = new HashMap<>();
        if (context.inputData() != null) {
            output.putAll(context.inputData());
        }
        return ActionResult.success(output);
    }
}
