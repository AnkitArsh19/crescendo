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
@ActionMapping(appKey = "logic", actionKey = "switch")
public class SwitchActionHandler implements ActionHandler {
    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Object value = config.containsKey("value")
                ? config.get("value")
                : LogicUtils.valueAt(context.inputData(), String.valueOf(config.getOrDefault("field", "")));
        String selectedBranch = String.valueOf(config.getOrDefault("defaultBranchKey", "default"));

        Object casesObj = config.get("cases");
        if (casesObj instanceof List<?> cases) {
            for (Object caseObj : cases) {
                if (caseObj instanceof Map<?, ?> c) {
                    Object expected = c.get("value");
                    Object operatorValue = c.containsKey("operator") ? c.get("operator") : "equals";
                    String operator = String.valueOf(operatorValue);
                    if (LogicUtils.matches(value, operator, expected)) {
                        Object branchValue = c.containsKey("branchKey") ? c.get("branchKey") : expected;
                        selectedBranch = String.valueOf(branchValue);
                        break;
                    }
                }
            }
        }

        Map<String, Object> output = new HashMap<>(context.inputData());
        output.put("selectedBranch", selectedBranch);
        output.put("_branchKey", selectedBranch);
        return ActionResult.success(output);
    }
}
