package com.crescendo.emailservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
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
 * When a real EmailService is wired, replace the calls to this class with it
 * and delete (or disable) this file.
 */
@Component
@Profile("local")
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
}
