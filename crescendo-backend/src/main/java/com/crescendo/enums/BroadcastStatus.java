package com.crescendo.enums;

/**
 * Status of a broadcast email job.
 */
public enum BroadcastStatus {
    DRAFT,      // Created but not yet sent
    SENDING,    // Currently fanning out emails to contacts
    COMPLETED,  // All emails queued successfully
    FAILED      // Fan-out failed partway through
}
