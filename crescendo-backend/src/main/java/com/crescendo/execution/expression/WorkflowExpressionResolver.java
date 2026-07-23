package com.crescendo.execution.expression;

import com.crescendo.steps.steps_command.Steps_command;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves workflow values without losing their native type.
 *
 * <p>References may be represented as a structured value
 * {@code {"$ref":{"step":"<uuid>","path":"payload.id"}}} or as the
 * backwards-compatible string form {@code {{steps.<uuid>.payload.id}}}. An exact
 * string reference resolves to the underlying object; references embedded in text
 * resolve to their string representation. Resolution is recursive for maps and lists.
 */
@Component
public class WorkflowExpressionResolver {

    private static final Pattern STEP_REFERENCE = Pattern.compile(
            "\\{\\{steps\\.([0-9a-fA-F-]{36}|\\d+)\\.([^}]+)}}");

    public Map<String, Object> resolveConfiguration(Map<String, Object> configuration,
                                                     Map<UUID, Map<String, Object>> outputs,
                                                     Map<UUID, Steps_command> stepsById) {
        if (configuration == null || configuration.isEmpty()) return Map.of();
        Object resolved = resolve(configuration, outputs, stepsById);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resolved;
        return result;
    }

    public Object resolve(Object value, Map<UUID, Map<String, Object>> outputs,
                          Map<UUID, Steps_command> stepsById) {
        if (value instanceof Map<?, ?> rawMap) {
            if (rawMap.size() == 1 && rawMap.get("$ref") instanceof Map<?, ?> ref) {
                Object path = ref.get("path");
                return resolveReference(String.valueOf(ref.get("step")),
                        String.valueOf(path != null ? path : ""), outputs, stepsById);
            }
            Map<String, Object> result = new LinkedHashMap<>();
            rawMap.forEach((key, child) -> result.put(String.valueOf(key), resolve(child, outputs, stepsById)));
            return result;
        }
        if (value instanceof List<?> list) {
            List<Object> result = new ArrayList<>(list.size());
            for (Object child : list) result.add(resolve(child, outputs, stepsById));
            return result;
        }
        if (value instanceof String text) return resolveString(text, outputs, stepsById);
        return value;
    }

    private Object resolveString(String text, Map<UUID, Map<String, Object>> outputs,
                                 Map<UUID, Steps_command> stepsById) {
        Matcher matcher = STEP_REFERENCE.matcher(text);
        if (!matcher.find()) return text;

        // Preserve objects, numbers, booleans and lists when a reference is the entire value.
        if (matcher.start() == 0 && matcher.end() == text.length()) {
            return resolveReference(matcher.group(1), matcher.group(2), outputs, stepsById);
        }

        matcher.reset();
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            Object replacement = resolveReference(matcher.group(1), matcher.group(2), outputs, stepsById);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement == null ? "" : String.valueOf(replacement)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private Object resolveReference(String stepReference, String path,
                                    Map<UUID, Map<String, Object>> outputs,
                                    Map<UUID, Steps_command> stepsById) {
        UUID stepId = parseStepId(stepReference, stepsById);
        if (stepId == null) return null;
        Object current = outputs.get(stepId);
        if (path == null || path.isBlank()) return current;
        for (String part : path.split("\\.")) {
            if (current instanceof Map<?, ?> map) current = map.get(part);
            else if (current instanceof List<?> list && part.matches("\\d+")) {
                int index = Integer.parseInt(part);
                current = index >= 0 && index < list.size() ? list.get(index) : null;
            } else return null;
        }
        return current;
    }

    private UUID parseStepId(String reference, Map<UUID, Steps_command> stepsById) {
        try {
            return UUID.fromString(reference);
        } catch (IllegalArgumentException ignored) {
            // Legacy draft references use the persisted step order. This is deterministic,
            // unlike the previous first-output-that-has-the-field fallback.
            try {
                int order = Integer.parseInt(reference);
                return stepsById.entrySet().stream()
                        .filter(e -> e.getValue().getOrder() != null
                                && e.getValue().getOrder().intValue() == order)
                        .map(Map.Entry::getKey)
                        .findFirst().orElse(null);
            } catch (NumberFormatException ignoredAgain) {
                return null;
            }
        }
    }
}
