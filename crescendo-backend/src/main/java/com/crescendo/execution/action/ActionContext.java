package com.crescendo.execution.action;

import java.util.Map;

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
        Map<String, Object> inputData
) {
}
