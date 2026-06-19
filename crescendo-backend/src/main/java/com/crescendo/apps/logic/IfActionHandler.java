package com.crescendo.apps.logic;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ActionMapping(appKey = "logic", actionKey = "if")
public class IfActionHandler implements ActionHandler {
    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Object left = config.containsKey("left")
                ? config.get("left")
                : LogicUtils.valueAt(context.inputData(), String.valueOf(config.getOrDefault("field", "")));
        boolean matched = LogicUtils.matches(left, String.valueOf(config.getOrDefault("operator", "equals")), config.get("right"));

        Map<String, Object> output = new HashMap<>(context.inputData());
        output.put("matched", matched);
        output.put("_branchKey", matched ? "true" : "false");
        return ActionResult.success(output);
    }
}
