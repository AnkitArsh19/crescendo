package com.crescendo.emailservice.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Raised when an email bounces back from the recipient's mail server.
 */
public class EmailBouncedEvent extends BaseDomainEvent {

    /** Classification matching the Resend/mailbox-provider taxonomy. */
    public enum BounceType { TRANSIENT, PERMANENT, UNDETERMINED }

    private final UUID domainId;
    private final String bounceReason;
    private final BounceType bounceType;

    /** Legacy constructor — defaults to UNDETERMINED for backward compatibility. */
    public EmailBouncedEvent(UUID emailId, UUID domainId, String bounceReason) {
        this(emailId, domainId, bounceReason, BounceType.UNDETERMINED);
    }

    public EmailBouncedEvent(UUID emailId, UUID domainId, String bounceReason, BounceType bounceType) {
        super(emailId);
        this.domainId = domainId;
        this.bounceReason = bounceReason;
        this.bounceType = bounceType;
    }

    public UUID getDomainId() { return domainId; }
    public String getBounceReason() { return bounceReason; }
    public BounceType getBounceType() { return bounceType; }
}
