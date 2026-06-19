package com.crescendo.steps.steps_command;

import com.crescendo.enums.StepType;
import com.crescendo.shared.domain.valueobject.ActionKey;
import com.crescendo.shared.domain.valueobject.AppKey;
import com.crescendo.shared.domain.valueobject.StepOrder;
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
        @Index(name = "idx_steps_parent_branch", columnList = "workflowId, parentStepId, branchKey"),
        @Index(name = "idx_steps_deleted", columnList = "deletedAt")
    })
public class Steps_command {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflowId", referencedColumnName = "id", nullable = false, foreignKey = @ForeignKey(name = "fk_step_workflow"))
    private Workflow_command workflow;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "stepType", nullable = false, length = 20)
    private StepType type;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "step_order", nullable = false, precision = 18, scale = 6))
    private StepOrder order;

    @Column(name = "deletedAt")
    private Instant deletedAt;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "actionKey", nullable = false, length = 120))
    private ActionKey actionKey;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt", nullable = false)
    private Instant updatedAt;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "appKey", nullable = false, length = 100))
    private AppKey appKey;

    /**
     * Optional reference to user's connection for OAuth-based apps.
     * Can be null for apps that don't require authentication (e.g., webhooks).
     */
    @Column(name = "connectionId")
    private UUID connectionId;

    @Column(name = "parentStepId")
    private UUID parentStepId;

    @Column(name = "branchKey", length = 120)
    private String branchKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", columnDefinition = "jsonb")
    private Map<String, Object> configuration;

    public Steps_command() {
    }

    public Steps_command(UUID id, Workflow_command workflow, String name, StepType type, StepOrder order, 
                         ActionKey actionKey, AppKey appKey, UUID connectionId, Map<String, Object> configuration) {
        this.id = id;
        this.workflow = workflow;
        this.name = name;
        this.type = type;
        this.order = order;
        this.actionKey = actionKey;
        this.appKey = appKey;
        this.connectionId = connectionId;
        this.configuration = configuration;
    }

    /**
     * Convenience constructor accepting raw strings (validates internally).
     */
    public Steps_command(UUID id, Workflow_command workflow, String name, StepType type, BigDecimal order, 
                         String actionKey, String appKey, UUID connectionId, Map<String, Object> configuration) {
        this(id, workflow, name, type, StepOrder.of(order), ActionKey.of(actionKey), AppKey.of(appKey), connectionId, configuration);
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

    public StepOrder getOrderVO() {
        return order;
    }
    
    /**
     * Returns raw order as BigDecimal for compatibility.
     */
    public BigDecimal getOrder() {
        return order != null ? order.value() : null;
    }
    public void setOrder(StepOrder order) {
        this.order = order;
    }
    public void setOrder(BigDecimal order) {
        this.order = StepOrder.of(order);
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

    public ActionKey getActionKeyVO() {
        return actionKey;
    }

    /**
     * Returns raw action key string for compatibility.
     */
    public String getActionKey() {
        return actionKey != null ? actionKey.value() : null;
    }

    public void setActionKey(ActionKey actionKey) {
        this.actionKey = actionKey;
    }

    public void setActionKey(String actionKey) {
        this.actionKey = ActionKey.of(actionKey);
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

    public AppKey getAppKeyVO() {
        return appKey;
    }

    /**
     * Returns raw app key string for compatibility.
     */
    public String getAppKey() {
        return appKey != null ? appKey.value() : null;
    }

    public void setAppKey(AppKey appKey) {
        this.appKey = appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = AppKey.of(appKey);
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public UUID getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(UUID connectionId) {
        this.connectionId = connectionId;
    }

    public UUID getParentStepId() {
        return parentStepId;
    }

    public void setParentStepId(UUID parentStepId) {
        this.parentStepId = parentStepId;
    }

    public String getBranchKey() {
        return branchKey;
    }

    public void setBranchKey(String branchKey) {
        this.branchKey = branchKey;
    }
}
