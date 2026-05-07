package com.crescendo.enums;

/**
 * Status of an email send attempt.
 */
public enum EmailStatus {
    PENDING,      // Queued but not yet sent
    SENT,         // Sent to provider successfully
    DELIVERED,    // Confirmed delivered to recipient
    FAILED,       // Failed to send
    BOUNCED,      // Email bounced back
    SUPPRESSED    // Recipient is on the suppression list — email was not queued
}
