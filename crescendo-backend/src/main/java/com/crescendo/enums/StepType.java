package com.crescendo.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Type of step in a workflow.
 */
public enum StepType {
    TRIGGER,    // First step - initiates workflow on event
    ACTION;     // Subsequent steps - perform operations

    @JsonCreator
    public static StepType fromString(String value) {
        if (value == null) return null;
        String upper = value.trim().toUpperCase();
        if ("CONDITION".equals(upper) || "BRANCH".equals(upper) || "LOGIC".equals(upper)) {
            return ACTION;
        }
        return StepType.valueOf(upper);
    }
}
