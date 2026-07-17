package com.crescendo.workflow.domain_event;

import com.crescendo.shared.domain.event.BaseDomainEvent;

import java.util.UUID;

/**
 * Domain event raised when a workflow's step graph (steps and edges) is saved.
 *
 * Distinct from {@link WorkflowUpdatedEvent} which covers only metadata
 * (name, description) changes. Consumers that need to react specifically to
 * topology changes — such as re-validating webhook trigger registration when
 * a trigger step's app key is swapped — should listen to this event.
 *
 * Fired by {@code Workflow_commandService.saveGraph()}.
 */
public class WorkflowGraphSavedEvent extends BaseDomainEvent {

    private final UUID userId;

    public WorkflowGraphSavedEvent(UUID workflowId, UUID userId) {
        super(workflowId);
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
