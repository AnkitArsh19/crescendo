package com.crescendo.emailservice;

import com.crescendo.emailservice.provider.EmailMessage;
import com.crescendo.emailservice.provider.EmailProvider;
import com.crescendo.enums.EmailType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@ConditionalOnBean(EmailProvider.class)
public class ProdNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(ProdNotificationService.class);

    private final EmailProvider emailProvider;

    public ProdNotificationService(EmailProvider emailProvider) {
        this.emailProvider = emailProvider;
    }

    @Async
    public void sendAsync(String to, String from, String subject, String htmlBody) {
        try {
            EmailMessage msg = new EmailMessage(
                    to,
                    from,
                    subject,
                    htmlBody,
                    null,
                    null,
                    EmailType.TRANSACTIONAL,
                    UUID.randomUUID().toString()
            );
            emailProvider.send(msg);
            log.info("Sent transactional email '{}' from {} to {}", subject, from, to);
        } catch (Exception e) {
            log.error("Failed to send transactional email from {} to {}", from, to, e);
        }
    }

    @Override
    public void sendPasswordResetToken(String email, String plainToken) {
        String url = "https://app.crescendo.run/auth/reset-password?token=" + plainToken;
        sendAsync(email, "noreply@crescendo.run", "Reset your Crescendo password", EmailTemplateRenderer.renderPasswordReset(url));
    }

    @Override
    public void sendEmailVerificationToken(String email, String plainToken) {
        String url = "https://app.crescendo.run/auth/verify-email?token=" + plainToken;
        sendAsync(email, "noreply@crescendo.run", "Verify your Crescendo account", EmailTemplateRenderer.renderEmailVerification(url));
    }

    @Override
    public void sendWelcomeEmail(String email, String name) {
        sendAsync(email, "hello@crescendo.run", "Welcome to Crescendo!", EmailTemplateRenderer.renderWelcome(name));
    }

    @Override
    public void sendDeleteAccountEmail(String email) {
        sendAsync(email, "noreply@crescendo.run", "Your Crescendo account has been deleted", EmailTemplateRenderer.renderDeleteAccount());
    }

    @Override
    public void sendPasswordChangedEmail(String email) {
        sendAsync(email, "noreply@crescendo.run", "Your Crescendo password has been changed", EmailTemplateRenderer.renderPasswordChanged());
    }

    @Override
    public void sendTotpEnabledEmail(String email) {
        sendAsync(email, "noreply@crescendo.run", "Two-Factor Authentication enabled", EmailTemplateRenderer.renderTotpEnabled());
    }

    @Override
    public void sendTotpDisabledEmail(String email) {
        sendAsync(email, "noreply@crescendo.run", "Two-Factor Authentication disabled", EmailTemplateRenderer.renderTotpDisabled());
    }

    @Override
    public void sendLoginAlertEmail(String email, String device, String location) {
        sendAsync(email, "noreply@crescendo.run", "New login to your Crescendo account", EmailTemplateRenderer.renderLoginAlert(device, location));
    }
}
