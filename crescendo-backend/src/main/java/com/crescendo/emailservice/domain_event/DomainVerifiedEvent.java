package com.crescendo.emailservice.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Raised when a domain's DNS records are successfully verified.
 */
public class DomainVerifiedEvent extends BaseDomainEvent {

    private final String domainName;

    public DomainVerifiedEvent(UUID domainId, String domainName) {
        super(domainId);
        this.domainName = domainName;
    }

    public String getDomainName() { return domainName; }
}
