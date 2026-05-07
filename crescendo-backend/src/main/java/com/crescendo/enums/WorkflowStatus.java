package com.crescendo.enums;

/**
 * High-level status of a workflow definition (for query model).
 * Represents the last run status for display purposes.
 */
public enum WorkflowStatus {
    NEVER_RUN,  // Workflow has never been executed
    SUCCESS,    // Last run succeeded
    FAILED,     // Last run failed
    RUNNING     // Currently running
}
