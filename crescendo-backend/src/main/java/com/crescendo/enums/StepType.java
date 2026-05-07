package com.crescendo.enums;

/**
 * Type of step in a workflow.
 */
public enum StepType {
    TRIGGER,    // First step - initiates workflow on event
    ACTION      // Subsequent steps - perform operations
}
