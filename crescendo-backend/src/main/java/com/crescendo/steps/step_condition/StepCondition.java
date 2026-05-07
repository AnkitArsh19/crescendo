package com.crescendo.steps.step_condition;

import com.crescendo.enums.ConditionOperator;
import com.crescendo.steps.steps_command.Steps_command;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Defines conditions for trigger steps to filter incoming events.
 * When an event arrives, these conditions are checked to determine
 * if the workflow should be triggered.
 */
@Entity
@Table(name = "step_condition",
    indexes = {
        @Index(name = "idx_step_condition_step", columnList = "stepId")
    })
public class StepCondition {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stepId", referencedColumnName = "id", nullable = false, foreignKey = @ForeignKey(name = "fk_condition_step"))
    private Steps_command step;

    /**
     * The JSON path or field name in the incoming event data to evaluate.
     * Example: "message.text", "payload.action", "body.subject"
     */
    @Column(name = "field", nullable = false, length = 255)
    private String field;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false, length = 50)
    private ConditionOperator operator;

    /**
     * The value to compare against. For EXISTS/NOT_EXISTS operators, this can be null.
     */
    @Column(name = "value", length = 1000)
    private String value;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false, updatable = false)
    private Instant createdAt;

    public StepCondition() {
    }

    public StepCondition(UUID id, Steps_command step, String field, ConditionOperator operator, String value) {
        this.id = id;
        this.step = step;
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Steps_command getStep() {
        return step;
    }

    public void setStep(Steps_command step) {
        this.step = step;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public ConditionOperator getOperator() {
        return operator;
    }

    public void setOperator(ConditionOperator operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
