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
@ActionMapping(appKey = "logic", actionKey = "split-in-batches")
public class SplitInBatchesActionHandler implements ActionHandler {
    @Override
public ActionResult execute(ActionContext context) {
        String itemsPath = String.valueOf(context.configuration().getOrDefault("itemsPath", "items"));
        Object source = itemsPath == null || itemsPath.isBlank()
                ? context.inputData()
                : LogicUtils.valueAt(context.inputData(), itemsPath);
        if (!(source instanceof List<?> items)) {
            return ActionResult.failure("Split In Batches requires an input array");
        }

        int batchSize = parseInt(context.configuration().get("batchSize"), 10);
        int batchIndex = parseInt(context.configuration().get("batchIndex"), 0);
        int from = Math.max(0, batchIndex * batchSize);
        int to = Math.min(items.size(), from + batchSize);
        List<?> batch = from >= items.size() ? List.of() : items.subList(from, to);

        Map<String, Object> output = new HashMap<>(context.inputData());
        output.put("items", batch);
        output.put("batchIndex", batchIndex);
        output.put("batchSize", batchSize);
        output.put("count", batch.size());
        output.put("hasMore", to < items.size());
        output.put("nextBatchIndex", to < items.size() ? batchIndex + 1 : null);
        return ActionResult.success(output);
    }

    private int parseInt(Object value, int fallback) {
        if (value instanceof Number n) return n.intValue();
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
