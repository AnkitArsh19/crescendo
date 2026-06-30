package com.crescendo.emailservice;

/**
 * Notification abstraction for password reset and email verification flows.
 */
public interface NotificationService {

    void sendPasswordResetToken(String email, String plainToken);

    void sendEmailVerificationToken(String email, String plainToken);

    void sendWelcomeEmail(String email, String name);

    void sendDeleteAccountEmail(String email);

    void sendPasswordChangedEmail(String email);

    void sendTotpEnabledEmail(String email);

    void sendTotpDisabledEmail(String email);

    void sendLoginAlertEmail(String email, String device, String location);
}
