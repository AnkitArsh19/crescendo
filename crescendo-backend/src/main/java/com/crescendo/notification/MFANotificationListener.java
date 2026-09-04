package com.crescendo.notification;

import com.crescendo.user.domain_event.MFADisabledEvent;
import com.crescendo.user.domain_event.MFAEnabledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MFANotificationListener {

    private static final Logger log = LoggerFactory.getLogger(MFANotificationListener.class);

    private final UserNotificationService userNotificationService;

    public MFANotificationListener(UserNotificationService userNotificationService) {
        this.userNotificationService = userNotificationService;
    }

    @EventListener
    public void onMFAEnabled(MFAEnabledEvent event) {
        try {
            userNotificationService.create(
                    event.aggregateId(),
                    NotificationType.MFA_ENABLED,
                    "Two-Factor Authentication Enabled",
                    "Two-factor authentication (MFA) has been enabled for your account.",
                    Map.of("security", "mfa")
            );
        } catch (Exception e) {
            log.warn("Failed to create notification for MFAEnabledEvent: {}", e.getMessage());
        }
    }

    @EventListener
    public void onMFADisabled(MFADisabledEvent event) {
        try {
            userNotificationService.create(
                    event.aggregateId(),
                    NotificationType.MFA_DISABLED,
                    "Two-Factor Authentication Disabled",
                    "Two-factor authentication (MFA) has been disabled for your account.",
                    Map.of("security", "mfa")
            );
        } catch (Exception e) {
            log.warn("Failed to create notification for MFADisabledEvent: {}", e.getMessage());
        }
    }
}
