package com.crescendo.apps.set;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Replaces or merges fields into the current context based on configuration.
 */
@Component
@ActionMapping(appKey = "set", actionKey = "edit-fields")
@SuppressWarnings("unchecked")
public class SetActionHandler implements ActionHandler {

    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        
        if (config == null || !config.containsKey("fields")) {
            // If no fields are provided, it just passes the previous data through (no-op)
            return ActionResult.success(context.inputData());
        }

        Object fieldsObj = config.get("fields");
        if (!(fieldsObj instanceof Map)) {
            return ActionResult.failure("Set action requires 'fields' to be a valid JSON object");
        }

        Map<String, Object> newFields = (Map<String, Object>) fieldsObj;
        
        boolean keepOnlySet = false;
        if (config.containsKey("keepOnlySet") && config.get("keepOnlySet") != null) {
            keepOnlySet = Boolean.parseBoolean(String.valueOf(config.get("keepOnlySet")));
        }
        
        if (keepOnlySet) {
            return ActionResult.success(newFields);
        } else {
            // Merge into input data
            java.util.Map<String, Object> result = new java.util.HashMap<>(context.inputData());
            result.putAll(newFields);
            return ActionResult.success(result);
        }
    }
}
