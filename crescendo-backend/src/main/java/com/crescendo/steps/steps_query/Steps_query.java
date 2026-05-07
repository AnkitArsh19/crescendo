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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
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

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "stepType", nullable = false, length = 20)
    private StepType type;

    @Column(name = "step_order", nullable = false, precision = 18, scale = 6)
    private BigDecimal order;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt", nullable = false)
    private Instant updatedAt;

    @Column(name = "appKey", nullable = false, length = 100)
    private String appKey;

    @Column(name = "actionKey", nullable = false, length = 100)
    private String actionKey;

    @Column(name = "connectionId")
    private UUID connectionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", columnDefinition = "jsonb")
    private Map<String, Object> configuration;

    public Steps_query() {
    }

    public Steps_query(UUID id, UUID workflowId, String name, StepType type, BigDecimal order, String appKey, String actionKey, UUID connectionId, Map<String, Object> configuration) {
        this.id = id;
        this.workflowId = workflowId;
        this.name = name;
        this.type = type;
        this.order = order;
        this.appKey = appKey;
        this.actionKey = actionKey;
        this.connectionId = connectionId;
        this.configuration = configuration;
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

    public BigDecimal getOrder() {
        return order;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getAppKey() {
        return appKey;
    }

    public String getActionKey() {
        return actionKey;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public UUID getConnectionId() {
        return connectionId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(StepType type) {
        this.type = type;
    }

    public void setOrder(BigDecimal order) {
        this.order = order;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public void setActionKey(String actionKey) {
        this.actionKey = actionKey;
    }

    public void setConnectionId(UUID connectionId) {
        this.connectionId = connectionId;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }
}
