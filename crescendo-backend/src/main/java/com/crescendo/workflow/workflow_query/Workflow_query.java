package com.crescendo.workflow.workflow_query;

import com.crescendo.enums.WorkflowStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_query")
public class Workflow_query {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "userId", nullable = false)
    private UUID userId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WorkflowStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_run_at", nullable = false)
    private Instant lastRunAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "step_count")
    private int step_count;

    public Workflow_query() {
    }

    public Workflow_query(UUID id, String name, String description, UUID userId, boolean isActive, WorkflowStatus status, Instant createdAt, Instant lastRunAt, Instant updatedAt, int step_count) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.userId = userId;
        this.isActive = isActive;
        this.status = status;
        this.createdAt = createdAt;
        this.lastRunAt = lastRunAt;
        this.updatedAt = updatedAt;
        this.step_count = step_count;
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
}
