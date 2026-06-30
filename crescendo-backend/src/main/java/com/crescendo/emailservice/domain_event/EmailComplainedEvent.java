package com.crescendo.emailservice.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Raised when a recipient marks an email as spam (complaint).
 */
public class EmailComplainedEvent extends BaseDomainEvent {

    private final UUID domainId;
    private final String complaintReason;

    public EmailComplainedEvent(UUID emailId, UUID domainId, String complaintReason) {
        super(emailId);
        this.domainId = domainId;
        this.complaintReason = complaintReason;
    }

    public UUID getDomainId() { return domainId; }
    public String getComplaintReason() { return complaintReason; }
}
