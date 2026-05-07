package com.crescendo.workflow.workflow_query;

import com.crescendo.enums.WorkflowStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_query",
    indexes = {
        @Index(name = "idx_workflow_query_user", columnList = "userId"),
        @Index(name = "idx_workflow_query_active", columnList = "is_active")
    })
public class Workflow_query {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "userId")
    private UUID userId;

    @Column(name = "guestSessionId", length = 100)
    private String guestSessionId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkflowStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "step_count")
    private int step_count;

    public Workflow_query() {
    }

    public Workflow_query(UUID id, String name, String description, UUID userId, String guestSessionId, boolean isActive, WorkflowStatus status, int stepCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.userId = userId;
        this.guestSessionId = guestSessionId;
        this.isActive = isActive;
        this.status = status;
        this.step_count = stepCount;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getGuestSessionId() {
        return guestSessionId;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getLastRunAt() {
        return lastRunAt;
    }

    public int getStep_count() {
        return step_count;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public void setLastRunAt(Instant lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public void setStep_count(int step_count) {
        this.step_count = step_count;
    }
}
