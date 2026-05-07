package com.crescendo.emailservice.provider;

/**
 * Strategy interface for email delivery providers.
 *
 * Implementations:
 *   - {@link SmtpEmailProvider} — Jakarta Mail (works with any SMTP: Gmail, Mailgun, SES, etc.)
 *   - Future: SES API, Mailgun HTTP API, SendGrid API
 *
 * The active provider is resolved by the {@link com.crescendo.emailservice.queue.EmailQueueConsumer}
 * when processing an email from the Redis queue.
 */
public interface EmailProvider {

    /**
     * Sends an email through this provider.
     *
     * @param message the provider-agnostic email message
     * @return result indicating success/failure and provider message ID
     */
    EmailSendResult send(EmailMessage message);

    /**
     * Returns a short identifier for this provider (e.g. "smtp", "ses", "mailgun").
     * Stored in EmailLog.provider for tracking.
     */
    String providerName();
}
