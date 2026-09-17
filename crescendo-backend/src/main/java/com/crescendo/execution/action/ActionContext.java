package com.crescendo.execution.action;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable context passed to an ActionHandler during step execution.
 *
 * @param appKey         app identifier (e.g. "http", "slack", "gmail")
 * @param actionKey      action within the app (e.g. "send-message", "request")
 * @param configuration  step-level config from the workflow builder (URL, template, etc.)
 * @param credentials    decrypted connection credentials, or empty map if auth is NONE
 * @param inputData      output from the previous step (or trigger data for the first action step)
 */
public record ActionContext(
        String appKey,
        String actionKey,
        Map<String, Object> configuration,
        Map<String, Object> credentials,
        Map<String, Object> inputData,
        UUID workflowRunId,
        UUID userId,
        UUID stepId,
        int stepOrder
) {
    // ── Config param helpers ──────────────────────────────────────────

    public String getString(String key) {
        Object val = configuration != null ? configuration.get(key) : null;
        return val != null ? String.valueOf(val) : null;
    }

    public String getString(String key, String defaultValue) {
        String val = getString(key);
        return val != null ? val : defaultValue;
    }

    public Integer getInt(String key) {
        return getInt(key, 0);
    }

    public Integer getInt(String key, int defaultValue) {
        Object val = configuration != null ? configuration.get(key) : null;
        if (val instanceof Number n) return n.intValue();
        try { return val != null ? Integer.parseInt(String.valueOf(val)) : defaultValue; }
        catch (NumberFormatException e) { return defaultValue; }
    }

    public Boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public Boolean getBoolean(String key, boolean defaultValue) {
        Object val = configuration != null ? configuration.get(key) : null;
        if (val instanceof Boolean b) return b;
        if (val != null) return Boolean.parseBoolean(String.valueOf(val));
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getMap(String key) {
        Object val = configuration != null ? configuration.get(key) : null;
        return val instanceof Map ? (Map<String, Object>) val : Map.of();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key) {
        Object val = configuration != null ? configuration.get(key) : null;
        return val instanceof List ? (List<T>) val : List.of();
    }

    public Object get(String key) {
        return configuration != null ? configuration.get(key) : null;
    }

    // ── Credential helpers ────────────────────────────────────────────

    public String getCredential(String key) {
        if (credentials != null && credentials.containsKey(key)) {
            Object val = credentials.get(key);
            if (val != null && !String.valueOf(val).isBlank()) {
                return String.valueOf(val);
            }
        }
        // Fallback for equivalent authentication token keys (OAuth accessToken <-> apiKey <-> token)
        if (credentials != null && ("apiKey".equals(key) || "accessToken".equals(key) || "token".equals(key) || "apiToken".equals(key) || "personalToken".equals(key))) {
            for (String fallbackKey : new String[]{"accessToken", "apiKey", "token", "apiToken", "personalToken"}) {
                if (credentials.containsKey(fallbackKey)) {
                    Object val = credentials.get(fallbackKey);
                    if (val != null && !String.valueOf(val).isBlank()) {
                        return String.valueOf(val);
                    }
                }
            }
        }
        return null;
    }

    // ── Input data helpers ────────────────────────────────────────────

    /** Returns the full input data map from the previous step. Equivalent to inputData(). */
    public Map<String, Object> input() {
        return inputData != null ? inputData : Map.of();
    }

    public Object input(String key) {
        return inputData != null ? inputData.get(key) : null;
    }
}
