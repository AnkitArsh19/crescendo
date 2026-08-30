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

    private static final Pattern DYNAMIC_TIME_PATTERN = Pattern.compile(
            "\\{\\{\\$?now(?:\\s*([+-])\\s*(\\d+)\\s*([a-zA-Z]+))?\\}\\}", Pattern.CASE_INSENSITIVE);

    private static final Pattern TODAY_PATTERN = Pattern.compile("\\{\\{today\\}\\}", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\{\\{timestamp\\}\\}", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIMESTAMP_SEC_PATTERN = Pattern.compile("\\{\\{timestamp_sec\\}\\}", Pattern.CASE_INSENSITIVE);

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
        // 1. Check exact dynamic system tokens first
        String trimmed = text.trim();
        if (TIMESTAMP_PATTERN.matcher(trimmed).matches()) {
            return System.currentTimeMillis();
        }
        if (TIMESTAMP_SEC_PATTERN.matcher(trimmed).matches()) {
            return java.time.Instant.now().getEpochSecond();
        }
        if (TODAY_PATTERN.matcher(trimmed).matches()) {
            return java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();
        }

        Matcher timeMatcher = DYNAMIC_TIME_PATTERN.matcher(trimmed);
        if (timeMatcher.matches()) {
            return resolveDynamicTime(timeMatcher.group(1), timeMatcher.group(2), timeMatcher.group(3));
        }

        // 2. Resolve embedded dynamic time tokens inside strings
        String processed = text;
        processed = TODAY_PATTERN.matcher(processed).replaceAll(java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString());
        processed = TIMESTAMP_PATTERN.matcher(processed).replaceAll(String.valueOf(System.currentTimeMillis()));
        processed = TIMESTAMP_SEC_PATTERN.matcher(processed).replaceAll(String.valueOf(java.time.Instant.now().getEpochSecond()));

        Matcher embeddedTimeMatcher = DYNAMIC_TIME_PATTERN.matcher(processed);
        if (embeddedTimeMatcher.find()) {
            StringBuffer timeBuf = new StringBuffer();
            embeddedTimeMatcher.reset();
            while (embeddedTimeMatcher.find()) {
                String resolvedTime = resolveDynamicTime(embeddedTimeMatcher.group(1), embeddedTimeMatcher.group(2), embeddedTimeMatcher.group(3));
                embeddedTimeMatcher.appendReplacement(timeBuf, Matcher.quoteReplacement(resolvedTime));
            }
            embeddedTimeMatcher.appendTail(timeBuf);
            processed = timeBuf.toString();
        }

        // 3. Resolve step variable references {{steps.N.field}}
        Matcher matcher = STEP_REFERENCE.matcher(processed);
        if (!matcher.find()) return processed;

        // Preserve objects, numbers, booleans and lists when a reference is the entire value.
        if (matcher.start() == 0 && matcher.end() == processed.length()) {
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

    private String resolveDynamicTime(String sign, String amountStr, String unitStr) {
        java.time.Instant now = java.time.Instant.now();
        if (sign == null || amountStr == null || unitStr == null) {
            return now.toString();
        }

        long amount = Long.parseLong(amountStr);
        String unit = unitStr.toLowerCase();
        java.time.temporal.ChronoUnit chronoUnit;

        if (unit.startsWith("s")) {
            chronoUnit = java.time.temporal.ChronoUnit.SECONDS;
        } else if (unit.startsWith("m") && !unit.startsWith("mo")) {
            chronoUnit = java.time.temporal.ChronoUnit.MINUTES;
        } else if (unit.startsWith("h")) {
            chronoUnit = java.time.temporal.ChronoUnit.HOURS;
        } else if (unit.startsWith("d")) {
            chronoUnit = java.time.temporal.ChronoUnit.DAYS;
        } else if (unit.startsWith("w")) {
            chronoUnit = java.time.temporal.ChronoUnit.WEEKS;
        } else {
            chronoUnit = java.time.temporal.ChronoUnit.MINUTES;
        }

        java.time.Instant target = "+".equals(sign) ? now.plus(amount, chronoUnit) : now.minus(amount, chronoUnit);
        return target.toString();
    }

    /**
     * Resolves configuration in step test mode using provided sample input data.
     * Handles system dynamic time tokens, {{steps.N.path}}, {{steps.trigger.path}},
     * and direct {{path}} references against inputData.
     */
    public Map<String, Object> resolveForTest(Map<String, Object> configuration, Map<String, Object> inputData) {
        if (configuration == null || configuration.isEmpty()) return Map.of();
        Map<String, Object> sampleInput = inputData == null ? Map.of() : inputData;
        Object resolved = resolveTestValue(configuration, sampleInput);
        if (resolved instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        return Map.of();
    }

    private Object resolveTestValue(Object value, Map<String, Object> sampleInput) {
        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> result = new LinkedHashMap<>();
            rawMap.forEach((k, child) -> result.put(String.valueOf(k), resolveTestValue(child, sampleInput)));
            return result;
        }
        if (value instanceof List<?> list) {
            List<Object> result = new ArrayList<>(list.size());
            for (Object child : list) result.add(resolveTestValue(child, sampleInput));
            return result;
        }
        if (value instanceof String text) {
            return resolveTestString(text, sampleInput);
        }
        return value;
    }

    private static final Pattern GENERIC_VAR_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    private Object resolveTestString(String text, Map<String, Object> sampleInput) {
        String trimmed = text.trim();

        // 1. Exact system tokens
        if (TIMESTAMP_PATTERN.matcher(trimmed).matches()) return System.currentTimeMillis();
        if (TIMESTAMP_SEC_PATTERN.matcher(trimmed).matches()) return java.time.Instant.now().getEpochSecond();
        if (TODAY_PATTERN.matcher(trimmed).matches()) return java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();
        Matcher timeMatcher = DYNAMIC_TIME_PATTERN.matcher(trimmed);
        if (timeMatcher.matches()) {
            return resolveDynamicTime(timeMatcher.group(1), timeMatcher.group(2), timeMatcher.group(3));
        }

        // 2. Embedded system tokens
        String processed = text;
        processed = TODAY_PATTERN.matcher(processed).replaceAll(java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString());
        processed = TIMESTAMP_PATTERN.matcher(processed).replaceAll(String.valueOf(System.currentTimeMillis()));
        processed = TIMESTAMP_SEC_PATTERN.matcher(processed).replaceAll(String.valueOf(java.time.Instant.now().getEpochSecond()));

        Matcher embeddedTimeMatcher = DYNAMIC_TIME_PATTERN.matcher(processed);
        if (embeddedTimeMatcher.find()) {
            StringBuffer timeBuf = new StringBuffer();
            embeddedTimeMatcher.reset();
            while (embeddedTimeMatcher.find()) {
                String resolvedTime = resolveDynamicTime(embeddedTimeMatcher.group(1), embeddedTimeMatcher.group(2), embeddedTimeMatcher.group(3));
                embeddedTimeMatcher.appendReplacement(timeBuf, Matcher.quoteReplacement(resolvedTime));
            }
            embeddedTimeMatcher.appendTail(timeBuf);
            processed = timeBuf.toString();
        }

        // 3. Check for variable references {{...}}
        Matcher varMatcher = GENERIC_VAR_PATTERN.matcher(processed);
        if (!varMatcher.find()) return processed;

        // If entire string is a single {{var}}, preserve data type (numbers, booleans, objects)
        if (varMatcher.start() == 0 && varMatcher.end() == processed.length()) {
            Object val = extractFromSampleInput(varMatcher.group(1).trim(), sampleInput);
            return val != null ? val : processed;
        }

        varMatcher.reset();
        StringBuffer sb = new StringBuffer();
        while (varMatcher.find()) {
            String expr = varMatcher.group(1).trim();
            Object val = extractFromSampleInput(expr, sampleInput);
            String replacement = val != null ? String.valueOf(val) : varMatcher.group(0);
            varMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        varMatcher.appendTail(sb);
        return sb.toString();
    }

    private Object extractFromSampleInput(String expr, Map<String, Object> sampleInput) {
        if (sampleInput == null || sampleInput.isEmpty()) return null;

        // A. Direct exact match in sampleInput
        if (sampleInput.containsKey(expr)) {
            return sampleInput.get(expr);
        }

        // B. Handle steps.N.path or steps.trigger.path or step_id.path
        if (expr.startsWith("steps.")) {
            String sub = expr.substring("steps.".length());
            int dotIdx = sub.indexOf('.');
            if (dotIdx > 0) {
                String stepKey = sub.substring(0, dotIdx);
                String remainingPath = sub.substring(dotIdx + 1);

                // 1. Check sampleInput.get("steps") -> Map -> get(stepKey)
                Object stepsObj = sampleInput.get("steps");
                if (stepsObj instanceof Map<?, ?> stepsMap) {
                    Object stepNode = stepsMap.get(stepKey);
                    if (stepNode != null) {
                        Object resolved = extractNestedPath(remainingPath, stepNode);
                        if (resolved != null) return resolved;
                    }
                }
                // 2. Check sampleInput.get(stepKey)
                Object stepNode = sampleInput.get(stepKey);
                if (stepNode != null) {
                    Object resolved = extractNestedPath(remainingPath, stepNode);
                    if (resolved != null) return resolved;
                }
                // 3. Fallback: check if remainingPath exists directly in sampleInput
                Object fallback = extractNestedPath(remainingPath, sampleInput);
                if (fallback != null) return fallback;
            }
        }

        // C. General dotted path extraction (e.g. data.customer.email or user.name)
        return extractNestedPath(expr, sampleInput);
    }

    private Object extractNestedPath(String path, Object root) {
        if (root == null || path == null || path.isBlank()) return root;
        // Strip leading "data." or "payload." if applicable
        Object current = root;
        for (String part : path.split("\\.")) {
            if (part.isBlank()) continue;
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else if (current instanceof List<?> list && part.matches("\\d+")) {
                int index = Integer.parseInt(part);
                current = index >= 0 && index < list.size() ? list.get(index) : null;
            } else {
                return null;
            }
        }
        return current;
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
