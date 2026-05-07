package com.crescendo.execution.action;

import java.util.Map;

/**
 * Result returned by an ActionHandler after execution.
 *
 * @param success    true if the action executed without error
 * @param outputData data produced by this step (passed as input to the next step)
 * @param error      human-readable error message when {@code success} is false
 */
public record ActionResult(
        boolean success,
        Map<String, Object> outputData,
        String error
) {

    public static ActionResult success(Map<String, Object> outputData) {
        return new ActionResult(true, outputData, null);
    }

    public static ActionResult failure(String error) {
        return new ActionResult(false, Map.of(), error);
    }
}
