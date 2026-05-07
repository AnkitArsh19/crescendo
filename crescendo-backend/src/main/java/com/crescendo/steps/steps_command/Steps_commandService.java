package com.crescendo.steps.steps_command;

import com.crescendo.enums.StepType;
import com.crescendo.security.access.AccessControlService;
import com.crescendo.shared.domain.event.DomainEventPublisher;
import com.crescendo.shared.domain.valueobject.StepOrder;
import com.crescendo.steps.StepsDto;
import com.crescendo.steps.domain_event.StepCreatedEvent;
import com.crescendo.steps.domain_event.StepDeletedEvent;
import com.crescendo.steps.domain_event.StepReorderedEvent;
import com.crescendo.steps.domain_event.StepUpdatedEvent;
import com.crescendo.steps.step_condition.StepCondition;
import com.crescendo.steps.step_condition.StepConditionRepository;
import com.crescendo.steps.steps_query.Steps_query;
import com.crescendo.steps.steps_query.Steps_queryRepository;
import com.crescendo.workflow.WorkflowDto;
import com.crescendo.workflow.workflow_command.Workflow_command;
import com.crescendo.workflow.workflow_command.Workflow_commandRepository;
import com.crescendo.workflow.workflow_query.Workflow_queryRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Write-side service for step and step-condition management.
 *
 * Every mutation:
 *   1. Validates workflow ownership and access-tier limits
 *   2. Writes to the command database (source of truth)
 *   3. Synchronously projects changes to the query database
 *
 * Also handles cascade operations invoked by Workflow_commandService
 * when a workflow is soft-deleted.
 */
@Service
@Transactional
public class Steps_commandService {

    private final Steps_commandRepository stepRepo;
    private final Steps_queryRepository stepQueryRepo;
    private final Workflow_commandRepository workflowRepo;
    private final Workflow_queryRepository workflowQueryRepo;
    private final StepConditionRepository conditionRepo;
    private final AccessControlService accessControl;
    private final DomainEventPublisher eventPublisher;

    public Steps_commandService(Steps_commandRepository stepRepo,
                                Steps_queryRepository stepQueryRepo,
                                Workflow_commandRepository workflowRepo,
                                Workflow_queryRepository workflowQueryRepo,
                                StepConditionRepository conditionRepo,
                                AccessControlService accessControl,
                                DomainEventPublisher eventPublisher) {
        this.stepRepo = stepRepo;
        this.stepQueryRepo = stepQueryRepo;
        this.workflowRepo = workflowRepo;
        this.workflowQueryRepo = workflowQueryRepo;
        this.conditionRepo = conditionRepo;
        this.accessControl = accessControl;
        this.eventPublisher = eventPublisher;
    }

    // =====================================================================
    // STEP CRUD — AUTHENTICATED
    // =====================================================================

    /**
     * Adds a new step to a workflow owned by the authenticated user.
     * The step is appended at the end by default (order = currentMaxOrder + 1).
     * Enforces the per-tier step limit before persisting.
     */
    public WorkflowDto.StepResponse addStep(UUID userId, UUID workflowId,
                                            WorkflowDto.CreateStepRequest req) {
        Workflow_command workflow = findOwnedWorkflow(userId, workflowId);
        return createStep(workflow, workflowId, req);
    }

    /**
     * Updates an existing step's configuration.
     * Only non-null fields in the request are applied (partial update).
     */
    public void updateStep(UUID userId, UUID workflowId, UUID stepId,
                           WorkflowDto.UpdateStepRequest req) {
        findOwnedWorkflow(userId, workflowId);
        Steps_command step = findActiveStep(stepId, workflowId);
        applyStepUpdate(step, req);
        syncStepUpdateToQuery(stepId, req);
    }

    /**
     * Soft-deletes a step and removes its conditions and query projection.
     */
    public void deleteStep(UUID userId, UUID workflowId, UUID stepId) {
        findOwnedWorkflow(userId, workflowId);
        performStepDeletion(stepId, workflowId);
    }

    /**
     * Changes a step's position within its workflow using fractional ordering.
     */
    public void reorderStep(UUID userId, UUID workflowId, UUID stepId,
                            WorkflowDto.ReorderStepRequest req) {
        findOwnedWorkflow(userId, workflowId);
        performStepReorder(stepId, workflowId, req);
    }

    // =====================================================================
    // STEP CRUD — GUEST
    // =====================================================================

    /**
     * Adds a step to a guest-owned workflow.
     */
    public WorkflowDto.StepResponse addGuestStep(String guestSessionId, UUID workflowId,
                                                  WorkflowDto.CreateStepRequest req) {
        Workflow_command workflow = findGuestOwnedWorkflow(guestSessionId, workflowId);
        return createStep(workflow, workflowId, req);
    }

    /**
     * Updates a step in a guest-owned workflow.
     */
    public void updateGuestStep(String guestSessionId, UUID workflowId, UUID stepId,
                                WorkflowDto.UpdateStepRequest req) {
        findGuestOwnedWorkflow(guestSessionId, workflowId);
        Steps_command step = findActiveStep(stepId, workflowId);
        applyStepUpdate(step, req);
        syncStepUpdateToQuery(stepId, req);
    }

    /**
     * Soft-deletes a step from a guest-owned workflow.
     */
    public void deleteGuestStep(String guestSessionId, UUID workflowId, UUID stepId) {
        findGuestOwnedWorkflow(guestSessionId, workflowId);
        performStepDeletion(stepId, workflowId);
    }

    /**
     * Reorders a step in a guest-owned workflow.
     */
    public void reorderGuestStep(String guestSessionId, UUID workflowId, UUID stepId,
                                  WorkflowDto.ReorderStepRequest req) {
        findGuestOwnedWorkflow(guestSessionId, workflowId);
        performStepReorder(stepId, workflowId, req);
    }

    // =====================================================================
    // CONDITION CRUD — AUTHENTICATED
    // =====================================================================

    /**
     * Adds a filter condition to a TRIGGER step.
     * Conditions are evaluated at runtime to decide whether an incoming
     * event should start the workflow.
     *
     * @throws ResponseStatusException 400 if the step is not a TRIGGER step
     */
    public StepsDto.ConditionResponse addCondition(UUID userId, UUID workflowId, UUID stepId,
                                                    StepsDto.CreateConditionRequest req) {
        findOwnedWorkflow(userId, workflowId);
        Steps_command step = findActiveStep(stepId, workflowId);
        return createCondition(step, req);
    }

    /**
     * Removes a condition from a step.
     */
    public void deleteCondition(UUID userId, UUID workflowId, UUID stepId, UUID conditionId) {
        findOwnedWorkflow(userId, workflowId);
        findActiveStep(stepId, workflowId);
        removeCondition(stepId, conditionId);
    }

    // =====================================================================
    // CONDITION CRUD — GUEST
    // =====================================================================

    /**
     * Adds a condition to a TRIGGER step in a guest-owned workflow.
     */
    public StepsDto.ConditionResponse addGuestCondition(String guestSessionId, UUID workflowId,
                                                         UUID stepId, StepsDto.CreateConditionRequest req) {
        findGuestOwnedWorkflow(guestSessionId, workflowId);
        Steps_command step = findActiveStep(stepId, workflowId);
        return createCondition(step, req);
    }

    /**
     * Removes a condition from a step in a guest-owned workflow.
     */
    public void deleteGuestCondition(String guestSessionId, UUID workflowId,
                                      UUID stepId, UUID conditionId) {
        findGuestOwnedWorkflow(guestSessionId, workflowId);
        findActiveStep(stepId, workflowId);
        removeCondition(stepId, conditionId);
    }

    // =====================================================================
    // CASCADE OPERATIONS (called by Workflow_commandService)
    // =====================================================================

    /**
     * Soft-deletes all steps for a workflow, removes their conditions,
     * and cleans up query-side projections.
     * Called when a workflow is being soft-deleted.
     */
    public void softDeleteAllForWorkflow(UUID workflowId) {
        Instant now = Instant.now();
        List<Steps_command> steps = stepRepo.findActiveByWorkflowId(workflowId);
        for (Steps_command step : steps) {
            conditionRepo.deleteByStepId(step.getId());
            step.setDeletedAt(now);
        }
        stepQueryRepo.deleteAllByWorkflowId(workflowId);
    }

    // =====================================================================
    // INTERNAL — SHARED STEP OPERATIONS
    // =====================================================================

    /// Core step creation logic shared by authenticated and guest paths.
    private WorkflowDto.StepResponse createStep(Workflow_command workflow, UUID workflowId,
                                                WorkflowDto.CreateStepRequest req) {
        int currentStepCount = (int) stepRepo.countByWorkflow_IdAndDeletedAtIsNull(workflowId);
        accessControl.enforceStepLimit(currentStepCount);

        StepOrder order = calculateNextOrder(workflowId);

        UUID stepId = UUID.randomUUID();
        Steps_command step = new Steps_command(
                stepId, workflow, req.name(), req.type(), order.value(),
                req.actionKey(), req.appKey(), req.connectionId(), req.configuration());
        stepRepo.save(step);

        projectStepToQuery(step, workflowId);
        updateQueryStepCount(workflowId);

        eventPublisher.publish(new StepCreatedEvent(stepId, workflowId, req.name()));

        return toStepResponse(step);
    }

    private void applyStepUpdate(Steps_command step, WorkflowDto.UpdateStepRequest req) {
        if (req.name() != null) step.setName(req.name());
        if (req.actionKey() != null) step.setActionKey(req.actionKey());
        if (req.appKey() != null) step.setAppKey(req.appKey());
        if (req.connectionId() != null) step.setConnectionId(req.connectionId());
        if (req.configuration() != null) step.setConfiguration(req.configuration());
        eventPublisher.publish(new StepUpdatedEvent(step.getId(), step.getWorkflow().getId()));
    }

    private void syncStepUpdateToQuery(UUID stepId, WorkflowDto.UpdateStepRequest req) {
        Steps_query queryStep = stepQueryRepo.findById(stepId).orElse(null);
        if (queryStep != null) {
            if (req.name() != null) queryStep.setName(req.name());
            if (req.appKey() != null) queryStep.setAppKey(req.appKey());
            if (req.actionKey() != null) queryStep.setActionKey(req.actionKey());
            if (req.connectionId() != null) queryStep.setConnectionId(req.connectionId());
            if (req.configuration() != null) queryStep.setConfiguration(req.configuration());
        }
    }

    private void performStepDeletion(UUID stepId, UUID workflowId) {
        Steps_command step = findActiveStep(stepId, workflowId);
        conditionRepo.deleteByStepId(stepId);
        step.setDeletedAt(Instant.now());
        stepQueryRepo.deleteById(stepId);
        updateQueryStepCount(workflowId);
        eventPublisher.publish(new StepDeletedEvent(stepId, workflowId));
    }

    private void performStepReorder(UUID stepId, UUID workflowId, WorkflowDto.ReorderStepRequest req) {
        Steps_command step = findActiveStep(stepId, workflowId);
        StepOrder newOrder = StepOrder.of(req.newOrder());
        step.setOrder(newOrder);

        Steps_query queryStep = stepQueryRepo.findById(stepId).orElse(null);
        if (queryStep != null) {
            queryStep.setOrder(newOrder.value());
        }
        eventPublisher.publish(new StepReorderedEvent(stepId, workflowId));
    }

    // =====================================================================
    // INTERNAL — CONDITION OPERATIONS
    // =====================================================================

    private StepsDto.ConditionResponse createCondition(Steps_command step, StepsDto.CreateConditionRequest req) {
        if (step.getType() != StepType.TRIGGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Conditions can only be added to TRIGGER steps");
        }

        StepCondition condition = new StepCondition(
                UUID.randomUUID(), step, req.field(), req.operator(), req.value());
        conditionRepo.save(condition);

        return toConditionResponse(condition);
    }

    private void removeCondition(UUID stepId, UUID conditionId) {
        StepCondition condition = conditionRepo.findById(conditionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Condition not found"));

        if (!condition.getStep().getId().equals(stepId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Condition not found on this step");
        }

        conditionRepo.delete(condition);
    }

    // =====================================================================
    // QUERY-SIDE PROJECTION
    // =====================================================================

    private void projectStepToQuery(Steps_command step, UUID workflowId) {
        Steps_query q = new Steps_query(
                step.getId(), workflowId, step.getName(), step.getType(),
                step.getOrder(), step.getAppKey(), step.getActionKey(), step.getConnectionId(), step.getConfiguration());
        stepQueryRepo.save(q);
    }

    private void updateQueryStepCount(UUID workflowId) {
        int count = (int) stepRepo.countByWorkflow_IdAndDeletedAtIsNull(workflowId);
        workflowQueryRepo.updateStepCount(workflowId, count);
    }

    // =====================================================================
    // OWNERSHIP VERIFICATION
    // =====================================================================

    private Workflow_command findOwnedWorkflow(UUID userId, UUID workflowId) {
        Workflow_command workflow = workflowRepo.findByIdAndDeletedAtIsNull(workflowId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));

        if (workflow.getUser() == null || !workflow.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this workflow");
        }
        return workflow;
    }

    private Workflow_command findGuestOwnedWorkflow(String guestSessionId, UUID workflowId) {
        Workflow_command workflow = workflowRepo.findByIdAndDeletedAtIsNull(workflowId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));

        if (workflow.getGuestSessionId() == null || !workflow.getGuestSessionId().equals(guestSessionId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this workflow");
        }
        return workflow;
    }

    private Steps_command findActiveStep(UUID stepId, UUID workflowId) {
        Steps_command step = stepRepo.findByIdAndDeletedAtIsNull(stepId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Step not found"));

        if (!step.getWorkflow().getId().equals(workflowId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Step not found in this workflow");
        }
        return step;
    }

    // =====================================================================
    // ORDERING
    // =====================================================================

    /// Calculates the next order value for a new step appended to the end.
    /// If no steps exist, starts at 1. Otherwise, takes the max order + 1.
    private StepOrder calculateNextOrder(UUID workflowId) {
        List<Steps_command> existing = stepRepo.findActiveByWorkflowIdOrdered(workflowId);
        if (existing.isEmpty()) {
            return StepOrder.of(1);
        }
        return existing.getLast().getOrderVO().nextWhole();
    }

    // =====================================================================
    // DTO MAPPERS
    // =====================================================================

    private WorkflowDto.StepResponse toStepResponse(Steps_command step) {
        return new WorkflowDto.StepResponse(
                step.getId().toString(),
                step.getName(),
                step.getType().name(),
                step.getOrder(),
                step.getAppKey(),
                step.getActionKey(),
                step.getConnectionId(),
                step.getConfiguration(),
                step.getCreatedAt(),
                step.getUpdatedAt()
        );
    }

    private StepsDto.ConditionResponse toConditionResponse(StepCondition condition) {
        return new StepsDto.ConditionResponse(
                condition.getId(),
                condition.getField(),
                condition.getOperator().name(),
                condition.getValue(),
                condition.getCreatedAt()
        );
    }
}
