package com.crescendo.notification.preference;

import com.crescendo.notification.NotificationType;

public record NotificationPreferenceDto(
        NotificationType type,
        String displayName,
        String category,
        boolean enabled
) {
    public static NotificationPreferenceDto of(NotificationType type, boolean enabled) {
        String category = getCategoryForType(type);
        return new NotificationPreferenceDto(
                type,
                formatDisplayName(type),
                category,
                enabled
        );
    }

    private static String getCategoryForType(NotificationType type) {
        return switch (type) {
            case WORKFLOW_RUN_SUCCESS, WORKFLOW_RUN_FAILED, WORKFLOW_RUN_CANCELLED -> "Workflows";
            case AI_WORKFLOW_GENERATED, AI_WORKFLOW_GENERATION_FAILED -> "AI Workflow Generator";
            case LOGIN_NEW_DEVICE, LOGIN_SUSPICIOUS, MFA_ENABLED, MFA_DISABLED -> "Security & Authentication";
            case CONNECTION_TOKEN_EXPIRED, CONNECTION_RECONNECTED -> "App Connections";
            case SYSTEM_ANNOUNCEMENT -> "System Announcements";
        };
    }

    private static String formatDisplayName(NotificationType type) {
        return switch (type) {
            case WORKFLOW_RUN_SUCCESS -> "Workflow Run Succeeded";
            case WORKFLOW_RUN_FAILED -> "Workflow Run Failed";
            case WORKFLOW_RUN_CANCELLED -> "Workflow Run Cancelled";
            case AI_WORKFLOW_GENERATED -> "AI Workflow Generated";
            case AI_WORKFLOW_GENERATION_FAILED -> "AI Workflow Generation Failed";
            case LOGIN_NEW_DEVICE -> "New Device Login";
            case LOGIN_SUSPICIOUS -> "Suspicious Login Activity";
            case MFA_ENABLED -> "Two-Factor Auth Enabled";
            case MFA_DISABLED -> "Two-Factor Auth Disabled";
            case CONNECTION_TOKEN_EXPIRED -> "App Connection Token Expired";
            case CONNECTION_RECONNECTED -> "App Connection Reconnected";
            case SYSTEM_ANNOUNCEMENT -> "System Announcements";
        };
    }
}
