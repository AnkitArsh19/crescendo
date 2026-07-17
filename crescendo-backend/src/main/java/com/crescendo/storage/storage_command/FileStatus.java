package com.crescendo.storage.storage_command;

public enum FileStatus {
    /**
     * File has been uploaded but not yet attached to a saved workflow.
     * Subject to 24-hour cleanup.
     */
    PENDING,

    /**
     * File is attached to a saved workflow step.
     */
    COMMITTED,

    /**
     * For RELAY files: File has been successfully delivered and can be safely deleted.
     */
    CONSUMED
}
