package com.crescendo.apps.logic;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ActionMapping(appKey = "logic", actionKey = "merge")
public class MergeActionHandler implements ActionHandler {
    @Override
public ActionResult execute(ActionContext context) {
        String mode = String.valueOf(context.configuration().getOrDefault("mode", "merge"));
        Object data = context.configuration().get("data");

        if ("append".equalsIgnoreCase(mode)) {
            List<Object> output = new ArrayList<>();
            Object currentItems = context.inputData().get("items");
            if (currentItems instanceof List<?> list) output.addAll(list);
            else output.add(context.inputData());
            if (data instanceof List<?> list) output.addAll(list);
            else if (data != null) output.add(data);
            return ActionResult.success(Map.of("items", output, "count", output.size()));
        }

        Map<String, Object> output = new HashMap<>(context.inputData());
        if (data instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                output.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return ActionResult.success(output);
    }
}
