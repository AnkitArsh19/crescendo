package com.crescendo.notification;

/**
 * All notification types surfaced in the user inbox.
 * Used as the discriminator for icons, colors, deep-links, and preference toggles.
 */
public enum NotificationType {

    // ── Workflow Runs ─────────────────────────────────────────────────────────
    WORKFLOW_RUN_SUCCESS,
    WORKFLOW_RUN_FAILED,
    WORKFLOW_RUN_CANCELLED,

    // ── AI Generation ─────────────────────────────────────────────────────────
    AI_WORKFLOW_GENERATED,
    AI_WORKFLOW_GENERATION_FAILED,

    // ── Security / Auth ──────────────────────────────────────────────────────
    LOGIN_NEW_DEVICE,
    LOGIN_SUSPICIOUS,
    MFA_ENABLED,
    MFA_DISABLED,

    // ── Connections / OAuth ───────────────────────────────────────────────────
    CONNECTION_TOKEN_EXPIRED,
    CONNECTION_RECONNECTED,

    // ── System ───────────────────────────────────────────────────────────────
    SYSTEM_ANNOUNCEMENT,
}
