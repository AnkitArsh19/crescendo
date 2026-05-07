package com.crescendo.emailservice.provider;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * SMTP-based email provider using Spring's {@link JavaMailSender} (Jakarta Mail).
 *
 * Works with any SMTP server: Gmail SMTP, Mailgun SMTP, AWS SES SMTP, Postfix, etc.
 * Configured via standard Spring Mail properties:
 *   spring.mail.host, spring.mail.port, spring.mail.username, spring.mail.password
 *
 * Only activated when {@code spring.mail.host} is configured — otherwise the bean
 * is not created and the system falls back to logging-only mode.
 */
@Component
@ConditionalOnProperty(name = "spring.mail.host")
public class SmtpEmailProvider implements EmailProvider {

    private static final Logger logger = LoggerFactory.getLogger(SmtpEmailProvider.class);

    private final JavaMailSender mailSender;

    public SmtpEmailProvider(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public EmailSendResult send(EmailMessage message) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

            helper.setTo(message.to());
            helper.setFrom(message.from());
            helper.setSubject(message.subject());

            if (message.htmlBody() != null) {
                // true = isHtml
                helper.setText(
                        message.textBody() != null ? message.textBody() : "",
                        message.htmlBody()
                );
            } else if (message.textBody() != null) {
                helper.setText(message.textBody(), false);
            }

            // RFC-8058 one-click unsubscribe headers — added after helper setup
            // so we can call mime.setHeader() directly on the MimeMessage.
            if (message.listUnsubscribeHeader() != null) {
                mime.setHeader("List-Unsubscribe", message.listUnsubscribeHeader());
                mime.setHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");
            }

            mailSender.send(mime);

            // SMTP doesn't return a message ID in the same way API providers do.
            // We use the MimeMessage's generated Message-ID header.
            String messageId = mime.getMessageID();
            logger.debug("[smtp] Sent email to={} messageId={}", message.to(), messageId);

            return EmailSendResult.success(messageId);

        } catch (MessagingException e) {
            logger.error("[smtp] Failed to send email to={}: {}", message.to(), e.getMessage());
            return EmailSendResult.failure("SMTP error: " + e.getMessage());
        }
    }

    @Override
    public String providerName() {
        return "smtp";
    }
}
