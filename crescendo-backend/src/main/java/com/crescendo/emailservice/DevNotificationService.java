package com.crescendo.emailservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Development-only notification stand-in.
 *
 * Because we have no email domain, all flows that would normally send an email
 * instead print the relevant token/link directly to the application terminal.
 *
 * How to spot the output:
 *   [DEV] Password reset token for alice@example.com : <token>
 *   [DEV] Email verification token for alice@example.com : <token>
 *
 * Activated automatically when no EmailProvider bean is present.
 */
@Component
@ConditionalOnMissingBean(com.crescendo.emailservice.provider.EmailProvider.class)
public class DevNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(DevNotificationService.class);

    /// Prints the raw password-reset token to the terminal.
    /// In production this would be replaced by an email with a reset link.
    /// Token is valid for 1 hour; use it in POST /auth/reset-password {"resetToken":"<value>",...}
    public void sendPasswordResetToken(String email, String plainToken) {
        log.warn("╔══════════════════════════════════════════════════════════╗");
        log.warn("║  [DEV] PASSWORD RESET TOKEN                              ║");
        log.warn("╠══════════════════════════════════════════════════════════╣");
        log.warn("║  Email : {}",  email);
        log.warn("║  Token : {}",  plainToken);
        log.warn("╠══════════════════════════════════════════════════════════╣");
        log.warn("║  Use:  POST /auth/reset-password                         ║");
        log.warn("║  Body: {{ \"resetToken\": \"<token>\", \"newPassword\": \"...\" }} ║");
        log.warn("╚══════════════════════════════════════════════════════════╝");
    }

    /// Prints the raw email-verification token to the terminal along with a clickable
    /// frontend link so testing in the browser is a single click away.
    /// In production this would be replaced by an email with a verification link.
    /// Token is valid for 24 hours.
    public void sendEmailVerificationToken(String email, String plainToken) {
        String frontendLink = "http://localhost:5173/verify-email?token=" + plainToken;
        log.warn("╔══════════════════════════════════════════════════════════════════╗");
        log.warn("║  [DEV] EMAIL VERIFICATION TOKEN                                  ║");
        log.warn("╠══════════════════════════════════════════════════════════════════╣");
        log.warn("║  Email : {}", email);
        log.warn("║  Token : {}", plainToken);
        log.warn("║  Link  : {}", frontendLink);
        log.warn("╠══════════════════════════════════════════════════════════════════╣");
        log.warn("║  API:  POST /auth/verify-email?token=<token>                     ║");
        log.warn("╚══════════════════════════════════════════════════════════════════╝");
    }

    @Override
    public void sendWelcomeEmail(String email, String name) {
        log.info("[DEV] Welcome email sent to: {} (Name: {})", email, name);
    }

    @Override
    public void sendDeleteAccountEmail(String email) {
        log.info("[DEV] Delete account email sent to: {}", email);
    }

    @Override
    public void sendPasswordChangedEmail(String email) {
        log.info("[DEV] Password changed email sent to: {}", email);
    }

    @Override
    public void sendTotpEnabledEmail(String email) {
        log.info("[DEV] TOTP Enabled email sent to: {}", email);
    }

    @Override
    public void sendTotpDisabledEmail(String email) {
        log.info("[DEV] TOTP Disabled email sent to: {}", email);
    }

    @Override
    public void sendLoginAlertEmail(String email, String device, String location) {
        log.info("[DEV] Login alert email sent to: {} (Device: {}, Location: {})", email, device, location);
    }
}
