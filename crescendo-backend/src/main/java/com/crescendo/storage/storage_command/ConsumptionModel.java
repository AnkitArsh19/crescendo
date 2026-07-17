package com.crescendo.storage.storage_command;

public enum ConsumptionModel {
    /**
     * File is uploaded to be relayed to an external destination (e.g. Google Drive, Email attachment).
     * Does not count towards long-term quota. Once delivered, transitions to CONSUMED.
     */
    RELAY,

    /**
     * File is retained for AI/processing context. Stays on infrastructure for the lifetime
     * of the workflow step. Counts against storage quota.
     */
    RETAINED
}
