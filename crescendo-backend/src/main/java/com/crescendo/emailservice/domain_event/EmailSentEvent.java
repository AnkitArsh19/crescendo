package com.crescendo.emailservice.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Raised when an email is successfully sent to the provider.
 */
public class EmailSentEvent extends BaseDomainEvent {

    private final String provider;
    private final String providerMessageId;

    public EmailSentEvent(UUID emailId, String provider, String providerMessageId) {
        super(emailId);
        this.provider = provider;
        this.providerMessageId = providerMessageId;
    }

    public String getProvider() { return provider; }
    public String getProviderMessageId() { return providerMessageId; }
}
