package com.crescendo.steps.steps_workflow;

import com.crescendo.enums.StepType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "steps_command")
public class Steps_command {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "workflowId", nullable = false)
    private UUID workflowId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "stepType", nullable = false)
    private StepType type;

    @Column(name = "order", nullable = false)
    private Integer order;

    @Column(name = "actionKey", nullable = false)
    private String actionKey;

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

    public Steps_command() {
    }

    public Steps_command(UUID id, UUID workflowId, String name, StepType type, Integer order, String actionKey, Instant createdAt, Instant updatedAt, String appKey, Map<String, Object> configuration) {
        this.id = id;
        this.workflowId = workflowId;
        this.name = name;
        this.type = type;
        this.order = order;
        this.actionKey = actionKey;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.appKey = appKey;
        this.configuration = configuration;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(UUID workflowId) {
        this.workflowId = workflowId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public StepType getType() {
        return type;
    }

    public void setType(StepType type) {
        this.type = type;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getActionKey() {
        return actionKey;
    }

    public void setActionKey(String actionKey) {
        this.actionKey = actionKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }
}
