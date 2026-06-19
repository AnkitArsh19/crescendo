package com.crescendo.apps.logic;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ActionMapping(appKey = "logic", actionKey = "filter")
public class FilterActionHandler implements ActionHandler {
    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Object source = LogicUtils.valueAt(context.inputData(), String.valueOf(config.getOrDefault("itemsPath", "items")));
        if (!(source instanceof List<?>)) {
            source = context.inputData().get("items");
        }
        if (!(source instanceof List<?> items)) {
            return ActionResult.failure("Filter requires an input array at itemsPath or input.items");
        }

        String field = String.valueOf(config.getOrDefault("field", ""));
        String operator = String.valueOf(config.getOrDefault("operator", "equals"));
        Object right = config.get("right");

        List<?> filtered = items.stream()
                .filter(item -> {
                    Object left = config.containsKey("left") ? config.get("left") : LogicUtils.valueAt(item, field);
                    return LogicUtils.matches(left, operator, right);
                })
                .toList();

        Map<String, Object> output = new HashMap<>(context.inputData());
        output.put("items", filtered);
        output.put("count", filtered.size());
        return ActionResult.success(output);
    }
}
