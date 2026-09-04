package com.crescendo.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Periodically deletes notifications older than the configured retention period (default 90 days).
 */
@Component
public class NotificationCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(NotificationCleanupJob.class);

    private final UserNotificationRepository userNotificationRepository;
    private final int retentionDays;

    public NotificationCleanupJob(
            UserNotificationRepository userNotificationRepository,
            @Value("${crescendo.notifications.retention-days:90}") int retentionDays) {
        this.userNotificationRepository = userNotificationRepository;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "0 0 2 * * ?") // 2:00 AM daily
    @Transactional
    public void cleanupOldNotifications() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        log.info("Starting notification cleanup for entries older than {} days (cutoff: {})", retentionDays, cutoff);
        try {
            int deleted = userNotificationRepository.deleteOlderThan(cutoff);
            log.info("Notification cleanup completed: {} expired notifications removed.", deleted);
        } catch (Exception e) {
            log.error("Failed to clean up old notifications: {}", e.getMessage(), e);
        }
    }
}
