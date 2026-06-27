package com.crescendo.apps.logic;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

final class LogicUtils {

    private LogicUtils() {
    }

    static Object valueAt(Object source, String path) {
        if (path == null || path.isBlank() || ".".equals(path)) {
            return source;
        }
        Object current = source;
        for (String part : path.split("\\.")) {
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

    static boolean matches(Object left, String operator, Object right) {
        String op = operator == null || operator.isBlank() ? "equals" : operator.trim().toLowerCase();
        return switch (op) {
            case "equals", "equal", "eq", "==" -> Objects.equals(stringValue(left), stringValue(right));
            case "not_equals", "not equal", "ne", "!=" -> !Objects.equals(stringValue(left), stringValue(right));
            case "contains" -> stringValue(left).contains(stringValue(right));
            case "not_contains" -> !stringValue(left).contains(stringValue(right));
            case "starts_with" -> stringValue(left).startsWith(stringValue(right));
            case "ends_with" -> stringValue(left).endsWith(stringValue(right));
            case "greater_than", "gt", ">" -> number(left) > number(right);
            case "greater_or_equal", "gte", ">=" -> number(left) >= number(right);
            case "less_than", "lt", "<" -> number(left) < number(right);
            case "less_or_equal", "lte", "<=" -> number(left) <= number(right);
            case "exists" -> left != null;
            case "not_exists" -> left == null;
            case "is_empty" -> left == null || stringValue(left).isBlank();
            case "is_not_empty" -> left != null && !stringValue(left).isBlank();
            case "regex" -> Pattern.compile(stringValue(right)).matcher(stringValue(left)).find();
            case "not_regex" -> !Pattern.compile(stringValue(right)).matcher(stringValue(left)).find();
            default -> false;
        };
    }

    static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    static double number(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(stringValue(value));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
