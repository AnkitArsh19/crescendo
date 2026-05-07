package com.crescendo.emailservice.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Fallback email provider used in development when no SMTP server is configured.
 * Logs the email to the console instead of sending it.
 *
 * Activated automatically when {@code SmtpEmailProvider} is absent
 * (i.e., {@code spring.mail.host} is not set).
 */
@Component
@ConditionalOnMissingBean(SmtpEmailProvider.class)
public class LoggingEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailProvider.class);

    @Override
    public EmailSendResult send(EmailMessage message) {
        log.warn("╔══════════════════════════════════════════════════════════╗");
        log.warn("║  [DEV] EMAIL SENT (logging-only, no SMTP configured)     ║");
        log.warn("╠══════════════════════════════════════════════════════════╣");
        log.warn("║  To           : {}", message.to());
        log.warn("║  From         : {}", message.from());
        log.warn("║  Subject      : {}", message.subject());
        log.warn("║  HTML         : {} chars", message.htmlBody() != null ? message.htmlBody().length() : 0);
        log.warn("║  Text         : {} chars", message.textBody() != null ? message.textBody().length() : 0);
        if (message.listUnsubscribeHeader() != null) {
            log.warn("║  Unsubscribe  : {}", message.listUnsubscribeHeader());
        }
        log.warn("╚══════════════════════════════════════════════════════════╝");

        String fakeMessageId = "dev-" + UUID.randomUUID();
        return EmailSendResult.success(fakeMessageId);
    }

    @Override
    public String providerName() {
        return "logging";
    }
}
