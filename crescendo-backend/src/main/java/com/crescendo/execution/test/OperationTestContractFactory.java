package com.crescendo.execution.test;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Produces the baseline, non-mutating test contract for a catalog operation.
 *
 * <p>The contract is intentionally derived from the existing catalog schema so
 * every integration receives a safe default immediately. Integrations can add
 * a {@code testContract} to their catalog definition later when they need a
 * more specialised probe. The default never authorises a production handler
 * to run during setup checking.</p>
 */
@Component
public class OperationTestContractFactory {

    public Map<String, Object> create(String appKey, Map<String, Object> operation, boolean trigger) {
        if (operation != null && operation.get("testContract") instanceof Map<?, ?> existing) {
            return copyMap(existing);
        }

        String operationKey = string(operation, trigger ? "triggerKey" : "actionKey");
        String operationName = string(operation, "name");
        List<Map<String, Object>> schema = configSchema(operation);
        boolean hasResources = schema.stream().anyMatch(this::isDynamicResource);
        boolean localOnly = isLocalOnly(appKey);
        boolean readOperation = isReadOperation(operationKey, operationName);

        String setupPolicy;
        if (trigger) {
            setupPolicy = "READ_SAMPLE";
        } else if (localOnly) {
            setupPolicy = "LOCAL_SIMULATION";
        } else if (readOperation) {
            setupPolicy = "READ_SAMPLE";
        } else if (hasResources) {
            setupPolicy = "READ_TARGET";
        } else {
            setupPolicy = "CONFIG_ONLY";
        }

        List<Map<String, Object>> checks = new ArrayList<>();
        checks.add(Map.of("type", "CONNECTION_HEALTH"));
        checks.add(Map.of("type", "REQUIRED_FIELDS"));
        for (Map<String, Object> field : schema) {
            if (isDynamicResource(field)) {
                Map<String, Object> check = new LinkedHashMap<>();
                check.put("type", "RESOURCE_EXISTS");
                check.put("field", string(field, "key"));
                check.put("resourceType", string(field, "resourceType"));
                Object dependsOn = field.get("dependsOn");
                if (dependsOn != null) check.put("dependsOn", dependsOn);
                checks.add(check);
            }
        }

        Map<String, Object> contract = new LinkedHashMap<>();
        contract.put("setupPolicy", setupPolicy);
        contract.put("checks", checks);
        contract.put("supportsInputPreview", true);
        contract.put("liveTestAllowed", !trigger && !localOnly);
        contract.put("sideEffect", trigger || localOnly || readOperation ? "NONE" : sideEffect(operationKey, operationName));
        contract.put("liveTestWarning", liveTestWarning(operationName, operationKey, trigger, localOnly, readOperation));
        return contract;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> configSchema(Map<String, Object> operation) {
        if (operation == null || !(operation.get("configSchema") instanceof List<?> raw)) return List.of();
        List<Map<String, Object>> schema = new ArrayList<>();
        for (Object field : raw) {
            if (field instanceof Map<?, ?> map) schema.add(copyMap(map));
        }
        return schema;
    }

    private boolean isDynamicResource(Map<String, Object> field) {
        return "dynamic_dropdown".equals(string(field, "type")) && !string(field, "resourceType").isBlank();
    }

    private boolean isLocalOnly(String appKey) {
        return List.of("logic", "schedule", "wait", "delay", "formatter", "code", "filter").contains(appKey);
    }

    private boolean isReadOperation(String key, String name) {
        String value = (key + " " + name).toLowerCase(Locale.ROOT);
        return value.matches(".*\\b(get|list|search|find|read|fetch|lookup|retrieve|download)\\b.*")
                || value.contains(":get") || value.contains(":list") || value.contains(":search")
                || value.contains(":find") || value.contains(":read") || value.contains(":fetch");
    }

    private String sideEffect(String key, String name) {
        String value = (key + " " + name).toLowerCase(Locale.ROOT);
        if (value.matches(".*\\b(delete|remove|archive|clear)\\b.*")) return "DELETE";
        if (value.matches(".*\\b(update|edit|move|add label|mark)\\b.*")) return "UPDATE";
        if (value.matches(".*\\b(send|post|message|notify|publish|invite)\\b.*")) return "MESSAGE";
        if (value.matches(".*\\b(pay|charge|refund|purchase)\\b.*")) return "PAYMENT";
        return "CREATE";
    }

    private String liveTestWarning(String name, String key, boolean trigger, boolean localOnly, boolean readOperation) {
        if (trigger) return "Triggers use a read-only sample record during setup checking.";
        if (localOnly) return "This step is simulated locally and never calls an external service.";
        if (readOperation) return "This operation is read-only when checked with your selected connection.";
        String operation = !name.isBlank() ? name : key;
        return "A live run will perform '" + operation + "' using the displayed input and may change data in your connected app.";
    }

    private String string(Map<String, Object> value, String key) {
        if (value == null || value.get(key) == null) return "";
        return String.valueOf(value.get(key));
    }

    private Map<String, Object> copyMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }
}
