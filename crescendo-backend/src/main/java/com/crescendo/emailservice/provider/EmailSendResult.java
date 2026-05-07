package com.crescendo.emailservice.provider;

/**
 * Result returned by an {@link EmailProvider} after attempting to send an email.
 *
 * @param success            true if the provider accepted the email for delivery
 * @param providerMessageId  provider-assigned message ID (for tracking), null on failure
 * @param error              human-readable error description when {@code success} is false
 */
public record EmailSendResult(
        boolean success,
        String providerMessageId,
        String error
) {
    public static EmailSendResult success(String providerMessageId) {
        return new EmailSendResult(true, providerMessageId, null);
    }

    public static EmailSendResult failure(String error) {
        return new EmailSendResult(false, null, error);
    }
}
