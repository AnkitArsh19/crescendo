package com.crescendo.shared.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String template utility for {@code {{variable}}} interpolation.
 * Supports dot-notation for nested map access (e.g., {@code {{user.email}}}).
 *
 * Used by both the workflow EmailActionHandler and the public EmailSendService.
 */
public final class TemplateInterpolator {

    private static final Pattern TEMPLATE_VAR = Pattern.compile("\\{\\{\\s*(\\w+(?:\\.\\w+)*)\\s*}}");

    private TemplateInterpolator() {
    }

    /**
     * Replaces {@code {{variable}}} placeholders with values from the data map.
     * Dot-notation paths (e.g., {@code {{user.email}}}) navigate nested maps.
     * Unresolved placeholders are replaced with an empty string.
     */
    public static String interpolate(String template, Map<String, Object> data) {
        if (template == null || data == null || data.isEmpty())
            return template;

        Matcher matcher = TEMPLATE_VAR.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String path = matcher.group(1);
            Object resolved = resolvePath(path, data);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(
                    resolved != null ? resolved.toString() : ""));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static Object resolvePath(String path, Map<String, Object> data) {
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
}
