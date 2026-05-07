package com.crescendo.logbook.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.Map;
import java.util.UUID;

/**
 * Domain event raised when a workflow run is started.
 */
public class WorkflowRunStartedEvent extends BaseDomainEvent {

    private final UUID workflowId;
    private final UUID userId;
    private final Map<String, Object> triggerData;

    public WorkflowRunStartedEvent(UUID workflowRunId, UUID workflowId,
                                    UUID userId, Map<String, Object> triggerData) {
        super(workflowRunId);
        this.workflowId = workflowId;
        this.userId = userId;
        this.triggerData = triggerData;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public UUID getUserId() {
        return userId;
    }

    public Map<String, Object> getTriggerData() {
        return triggerData;
    }
}
