package com.crescendo.enums;

/**
 * Status of email domain verification.
 */
public enum DomainStatus {
    PENDING,    // Awaiting DNS verification
    VERIFIED,   // DNS records verified successfully
    FAILED      // Verification failed
}
