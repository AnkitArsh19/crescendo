package com.crescendo.steps.steps_command;

import com.crescendo.enums.StepType;
import com.crescendo.workflow.workflow_command.Workflow_command;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "steps_command",
    indexes = {
        @Index(name = "idx_steps_workflow", columnList = "workflowId"),
        @Index(name = "idx_steps_order", columnList = "workflowId, step_order"),
        @Index(name = "idx_steps_deleted", columnList = "deletedAt")
    })
public class Steps_command {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflowId", referencedColumnName = "id", nullable = false, foreignKey = @ForeignKey(name = "fk_step_workflow"))
    private Workflow_command workflow;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "stepType", nullable = false)
    private StepType type;

    @Column(name = "step_order", nullable = false, precision = 18, scale = 6)
    private BigDecimal order;

    @Column(name = "deletedAt")
    private Instant deletedAt;

    @Column(name = "actionKey", nullable = false, length = 120)
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

    public Steps_command(UUID id, Workflow_command workflow, String name, StepType type, BigDecimal order, String actionKey, Instant createdAt, Instant updatedAt, String appKey, Map<String, Object> configuration, Instant deletedAt) {
        this.id = id;
        this.workflow = workflow;
        this.name = name;
        this.type = type;
        this.order = order;
        this.actionKey = actionKey;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.appKey = appKey;
        this.configuration = configuration;
        this.deletedAt = deletedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Workflow_command getWorkflow() {
        return workflow;
    }
    public void setWorkflow(Workflow_command workflow) {
        this.workflow = workflow;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public BigDecimal getOrder() {
        return order;
    }
    public void setOrder(BigDecimal order) {
        this.order = order;
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
