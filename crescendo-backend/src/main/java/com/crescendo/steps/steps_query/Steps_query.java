package com.crescendo.steps.steps_query;

import com.crescendo.enums.StepType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
/// Indexes tell that when creating table create indexes for the given columns.
/// Here the selection process improves.
/// The index column is the name given, and it creates index from the column list given
@Table(name = "steps_query",
    indexes = {
        @Index(name = "idx_steps_query_workflow", columnList = "workflowId"),
        @Index(name = "idx_steps_query_order", columnList = "workflowId, step_order")
    })
public class Steps_query {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "workflowId", nullable = false)
    private UUID workflowId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "stepType", nullable = false)
    private StepType type;

    @Column(name = "step_order", nullable = false)
    private Integer order;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt", nullable = false)
    private Instant updatedAt;

    @Column(name = "appKey", nullable = false)
    private String appKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", columnDefinition = "jsonb")
    private Map<String, Object> configuration;

    public Steps_query() {
    }

    public Steps_query(UUID id, UUID workflowId, String name, StepType type, Integer order, Instant createdAt, Instant updatedAt, Map<String, Object> configuration, String appKey) {
        this.id = id;
        this.workflowId = workflowId;
        this.name = name;
        this.type = type;
        this.order = order;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.configuration = configuration;
        this.appKey = appKey;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public String getName() {
        return name;
    }

    public StepType getType() {
        return type;
    }

    public Integer getOrder() {
        return order;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getAppKey() {
        return appKey;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
