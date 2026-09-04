package com.crescendo.notification;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record UserNotificationDto(
        UUID id,
        NotificationType type,
        String title,
        String body,
        Map<String, Object> metadata,
        boolean isRead,
        Instant createdAt
) {
    public static UserNotificationDto from(UserNotification notification) {
        return new UserNotificationDto(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getMetadata(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
