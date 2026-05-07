package com.crescendo.execution.condition;

import com.crescendo.enums.ConditionOperator;
import com.crescendo.steps.step_condition.StepCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Evaluates {@link StepCondition} rules against an incoming event payload.
 * All conditions are ANDed — every condition must match for the evaluation to pass.
 * <p>
 * Supports dot-notation field paths for nested access (e.g. {@code "payload.action"}).
 */
@Service
public class ConditionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(ConditionEvaluator.class);

    /**
     * Returns {@code true} when every condition matches the payload,
     * or when the condition list is empty (no filtering).
     */
    public boolean evaluate(List<StepCondition> conditions, Map<String, Object> payload) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        if (payload == null || payload.isEmpty()) {
            // Non-empty conditions against an empty payload — can only pass for NOT_EXISTS
            return conditions.stream().allMatch(c ->
                    c.getOperator() == ConditionOperator.NOT_EXISTS);
        }

        for (StepCondition condition : conditions) {
            if (!matchesSingle(condition, payload)) {
                logger.debug("Condition failed: field={}, op={}, expected={}",
                        condition.getField(), condition.getOperator(), condition.getValue());
                return false;
            }
        }
        return true;
    }

    private boolean matchesSingle(StepCondition condition, Map<String, Object> payload) {
        String field = condition.getField();
        ConditionOperator op = condition.getOperator();
        String expected = condition.getValue();
        Object actual = resolveField(field, payload);

        return switch (op) {
            case EXISTS -> actual != null;
            case NOT_EXISTS -> actual == null;
            case EQUALS -> actual != null && actual.toString().equals(expected);
            case NOT_EQUALS -> actual == null || !actual.toString().equals(expected);
            case CONTAINS -> actual != null && actual.toString().contains(expected);
            case NOT_CONTAINS -> actual == null || !actual.toString().contains(expected);
            case STARTS_WITH -> actual != null && actual.toString().startsWith(expected);
            case ENDS_WITH -> actual != null && actual.toString().endsWith(expected);
            case GREATER_THAN -> compareNumeric(actual, expected) > 0;
            case LESS_THAN -> compareNumeric(actual, expected) < 0;
        };
    }

    /**
     * Resolves a dot-separated field path against a nested map.
     * E.g. {@code "message.text"} → payload.get("message").get("text").
     */
    private Object resolveField(String path, Map<String, Object> data) {
        String[] parts = path.split("\\.");
        Object current = data;
        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    private int compareNumeric(Object actual, String expected) {
        if (actual == null || expected == null) return -1;
        try {
            double a = Double.parseDouble(actual.toString());
            double e = Double.parseDouble(expected);
            return Double.compare(a, e);
        } catch (NumberFormatException ex) {
            // Fall back to string comparison
            return actual.toString().compareTo(expected);
        }
    }
}
