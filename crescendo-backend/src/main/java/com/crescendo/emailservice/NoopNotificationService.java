package com.crescendo.emailservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Production-safe notification service placeholder.
 * Replace with a real email sender in production.
 */
@Component
@Profile("!local")
public class NoopNotificationService implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NoopNotificationService.class);

    @Override
    public void sendPasswordResetToken(String email, String plainToken) {
        logger.info("[notifications] Password reset requested for {}", email);
    }

    @Override
    public void sendEmailVerificationToken(String email, String plainToken) {
        logger.info("[notifications] Email verification requested for {}", email);
    }
}
