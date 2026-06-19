package com.crescendo.enums;

/**
 * Status of a workflow run execution.
 */
public enum WorkflowRunStatus {
    PENDING,    // Queued for execution
    RUNNING,    // Currently executing steps
    SUCCESS,    // All steps completed successfully
    FAILED,     // One or more steps failed
    CANCELLED,  // Cancelled by user
    SUSPENDED   // Suspended asynchronously waiting for an external event or timer
}
