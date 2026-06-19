package com.crescendo.apps.renamekeys;

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
@ActionMapping(appKey = "rename-keys", actionKey = "rename")
@SuppressWarnings("unchecked")
public class RenameKeysActionHandler implements ActionHandler {

    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        Map<String, Object> prev = context.inputData();

        if (prev == null || prev.isEmpty()) {
            return ActionResult.success(new HashMap<>());
        }

        if (config == null || !config.containsKey("keyMap")) {
            return ActionResult.success(prev);
        }

        Object mapObj = config.get("keyMap");
        if (!(mapObj instanceof Map)) {
            return ActionResult.failure("Rename Keys requires 'keyMap' to be a valid JSON object");
        }

        Map<String, Object> keyMap = (Map<String, Object>) mapObj;
        
        // Deep copy the previous output to avoid mutating cached execution state
        Map<String, Object> result = deepCopyMap(prev);

        for (Map.Entry<String, Object> entry : keyMap.entrySet()) {
            String oldKeyPath = entry.getKey();
            String newKeyPath = String.valueOf(entry.getValue());

            Object value = deepGet(result, oldKeyPath);
            if (value != null) {
                deepSet(result, newKeyPath, value);
                deepRemove(result, oldKeyPath);
            }
        }

        return ActionResult.success(result);
    }

    private Object deepGet(Map<String, Object> map, String path) {
        String[] parts = path.split("\\.");
        Object current = map;
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    private void deepSet(Map<String, Object> map, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = map;
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (!current.containsKey(part) || !(current.get(part) instanceof Map)) {
                current.put(part, new HashMap<>());
            }
            current = (Map<String, Object>) current.get(part);
        }
        current.put(parts[parts.length - 1], value);
    }

    private void deepRemove(Map<String, Object> map, String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = map;
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (current.get(part) instanceof Map) {
                current = (Map<String, Object>) current.get(part);
            } else {
                return;
            }
        }
        current.remove(parts[parts.length - 1]);
    }

    private Map<String, Object> deepCopyMap(Map<String, Object> original) {
        if (original == null) return null;
        Map<String, Object> copy = new HashMap<>();
        for (Map.Entry<String, Object> entry : original.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                copy.put(entry.getKey(), deepCopyMap((Map<String, Object>) value));
            } else if (value instanceof List) {
                copy.put(entry.getKey(), deepCopyList((List<Object>) value));
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        return copy;
    }

    private List<Object> deepCopyList(List<Object> original) {
        if (original == null) return null;
        List<Object> copy = new ArrayList<>();
        for (Object value : original) {
            if (value instanceof Map) {
                copy.add(deepCopyMap((Map<String, Object>) value));
            } else if (value instanceof List) {
                copy.add(deepCopyList((List<Object>) value));
            } else {
                copy.add(value);
            }
        }
        return copy;
    }
}
