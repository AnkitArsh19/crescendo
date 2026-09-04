package com.crescendo.notification.workflow;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Per-workflow notification setting — lets users control how noisy
 * run completion notifications are per workflow.
 */
@Entity
@Table(
    name = "workflow_notification_setting",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_wf_notif_setting_user_wf",
        columnNames = {"userId", "workflowId"}
    )
)
public class WorkflowNotificationSetting {

    public enum WorkflowNotifyMode {
        /** Notify on both success and failure (default). */
        ALWAYS,
        /** Only notify on failure or cancellation. */
        FAILURE_ONLY,
        /** Never notify for this workflow. */
        NEVER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "userId", nullable = false)
    private UUID userId;

    @Column(name = "workflowId", nullable = false)
    private UUID workflowId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notifyMode", nullable = false, length = 32)
    private WorkflowNotifyMode notifyMode = WorkflowNotifyMode.ALWAYS;

    protected WorkflowNotificationSetting() {}

    public WorkflowNotificationSetting(UUID userId, UUID workflowId, WorkflowNotifyMode mode) {
        this.userId = userId;
        this.workflowId = workflowId;
        this.notifyMode = mode;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getWorkflowId() { return workflowId; }
    public WorkflowNotifyMode getNotifyMode() { return notifyMode; }

    public void setNotifyMode(WorkflowNotifyMode notifyMode) { this.notifyMode = notifyMode; }
}
