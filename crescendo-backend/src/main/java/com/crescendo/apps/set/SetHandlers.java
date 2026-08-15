package com.crescendo.apps.set;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Set node handler — modifies, adds, or transforms fields on incoming workflow data.
 */
@Component
public class SetHandlers {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ActionMapping(appKey = "set", actionKey = "set:set")
    public Object set(ActionContext context) throws Exception {
        String mode = context.getString("mode");
        if (mode == null || mode.isBlank()) mode = "manual";

        Map<String, Object> result = new LinkedHashMap<>();
        
        // Start with input data unless keepOnlySet is true
        Map<String, Object> options = context.getMap("options");
        boolean keepOnlySet = options != null && Boolean.TRUE.equals(options.get("keepOnlySet"));

        if (!keepOnlySet && context.inputData() != null) {
            result.putAll(context.inputData());
        }

        if ("raw".equalsIgnoreCase(mode) || "json".equalsIgnoreCase(mode)) {
            String jsonOutput = context.getString("jsonOutput");
            if (jsonOutput != null && !jsonOutput.isBlank()) {
                try {
                    Map<String, Object> parsed = MAPPER.readValue(jsonOutput, new TypeReference<Map<String, Object>>() {});
                    if (keepOnlySet) {
                        result.clear();
                    }
                    result.putAll(parsed);
                } catch (Exception e) {
                    result.put("rawOutput", jsonOutput);
                }
            }
        } else {
            // Manual mode: fields can be a Map<String, Object> or List<Map<String, Object>>
            Object rawFields = context.get("fields");
            if (rawFields instanceof Map<?, ?> map) {
                map.forEach((k, v) -> {
                    if (k != null) result.put(k.toString(), v);
                });
            } else if (rawFields instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> fieldMap) {
                        Object name = fieldMap.get("name");
                        Object value = fieldMap.get("value");
                        if (name == null) name = fieldMap.get("key");
                        if (name != null) {
                            result.put(name.toString(), value);
                        }
                    }
                }
            } else if (rawFields instanceof String str && !str.isBlank()) {
                try {
                    Map<String, Object> parsed = MAPPER.readValue(str, new TypeReference<Map<String, Object>>() {});
                    result.putAll(parsed);
                } catch (Exception ignored) {
                    result.put("fields", str);
                }
            }
        }

        return result;
    }
}
