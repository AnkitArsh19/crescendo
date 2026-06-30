package com.crescendo.emailservice.provider;

/**
 * Provider-agnostic email message that is passed to any {@link EmailProvider} implementation.
 * Contains all fields an SMTP or API-based provider needs to send the email.
 *
 * listUnsubscribeHeader — if non-null, the provider should set it as the
 *   RFC-8058 List-Unsubscribe header (e.g. "<https://host/unsubscribe?token=id>").
 *   SmtpEmailProvider also adds List-Unsubscribe-Post: List-Unsubscribe=One-Click.
 */
import com.crescendo.enums.EmailType;

public record EmailMessage(
        String to,
        String from,
        String subject,
        String htmlBody,
        String textBody,
        String listUnsubscribeHeader,
        EmailType emailType,
        String idempotencyKey
) {
    /** Convenience constructor without List-Unsubscribe, EmailType, or idempotencyKey (used in tests and fallback paths). */
    public EmailMessage(String to, String from, String subject, String htmlBody, String textBody) {
        this(to, from, subject, htmlBody, textBody, null, EmailType.TRANSACTIONAL, null);
    }
}
