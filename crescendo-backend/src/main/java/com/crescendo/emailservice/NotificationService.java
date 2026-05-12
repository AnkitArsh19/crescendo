package com.crescendo.emailservice;

/**
 * Notification abstraction for password reset and email verification flows.
 */
public interface NotificationService {

    void sendPasswordResetToken(String email, String plainToken);

    void sendEmailVerificationToken(String email, String plainToken);
}
