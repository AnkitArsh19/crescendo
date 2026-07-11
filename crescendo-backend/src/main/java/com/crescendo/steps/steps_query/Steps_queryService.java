package com.crescendo.steps.steps_query;

import com.crescendo.steps.StepsDto;
import com.crescendo.steps.step_condition.StepCondition;
import com.crescendo.steps.step_condition.StepConditionRepository;
import com.crescendo.workflow.WorkflowDto;
import com.crescendo.workflow.workflow_query.Workflow_queryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Read-side service for step and step-condition queries.
 *
 * Step listing uses the query-side repo ({@link Steps_queryRepository})
 * for denormalized, read-optimized data. Step detail (with conditions)
 * still uses the command-side repo since conditions live there.
 *
 * Ownership is verified against the query-side workflow for efficiency.
 */
@Service
public class Steps_queryService {

    private final Steps_queryRepository stepQueryRepo;
    private final Workflow_queryRepository workflowQueryRepo;
    private final StepConditionRepository conditionRepo;

    public Steps_queryService(Steps_queryRepository stepQueryRepo,
                              Workflow_queryRepository workflowQueryRepo,
                              StepConditionRepository conditionRepo) {
        this.stepQueryRepo = stepQueryRepo;
        this.workflowQueryRepo = workflowQueryRepo;
        this.conditionRepo = conditionRepo;
    }

    // =====================================================================
    // STEP LISTING
    // =====================================================================

    /**
     * Returns ordered steps for a workflow owned by the authenticated user.
     */
    public List<WorkflowDto.StepResponse> getSteps(UUID userId, UUID workflowId) {
        verifyWorkflowOwnership(userId, workflowId);
        return loadSteps(workflowId);
    }

    /**
     * Returns ordered steps for a guest-owned workflow.
     */
    public List<WorkflowDto.StepResponse> getGuestSteps(String guestSessionId, UUID workflowId) {
        verifyGuestWorkflowOwnership(guestSessionId, workflowId);
        return loadSteps(workflowId);
    }

    // =====================================================================
    // STEP DETAIL (with conditions)
    // =====================================================================

    /**
     * Returns a single step with its conditions for an authenticated user.
     * Useful for step configuration views where the trigger conditions are needed.
     */
    public StepsDto.StepDetailResponse getStepDetail(UUID userId, UUID workflowId, UUID stepId) {
        verifyWorkflowOwnership(userId, workflowId);
        return loadStepDetail(stepId, workflowId);
    }

    /**
     * Returns a single step with its conditions for a guest user.
     */
    public StepsDto.StepDetailResponse getGuestStepDetail(String guestSessionId,
                                                           UUID workflowId, UUID stepId) {
        verifyGuestWorkflowOwnership(guestSessionId, workflowId);
        return loadStepDetail(stepId, workflowId);
    }

    // =====================================================================
    // CONDITION LISTING
    // =====================================================================

    /**
     * Returns conditions for a step owned by an authenticated user.
     */
    public List<StepsDto.ConditionResponse> getConditions(UUID userId, UUID workflowId, UUID stepId) {
        verifyWorkflowOwnership(userId, workflowId);
        verifyStepBelongsToWorkflow(stepId, workflowId);
        return loadConditions(stepId);
    }

    /**
     * Returns conditions for a step in a guest-owned workflow.
     */
    public List<StepsDto.ConditionResponse> getGuestConditions(String guestSessionId,
                                                                UUID workflowId, UUID stepId) {
        verifyGuestWorkflowOwnership(guestSessionId, workflowId);
        verifyStepBelongsToWorkflow(stepId, workflowId);
        return loadConditions(stepId);
    }

    // =====================================================================
    // SHARED LOADING (used by Workflow_queryService for detail views)
    // =====================================================================

    /**
     * Loads all active steps for a workflow from the query-side repo, ordered ascending.
     * Exposed publicly for use by Workflow_queryService when building workflow detail responses.
     */
    public List<WorkflowDto.StepResponse> loadSteps(UUID workflowId) {
        return stepQueryRepo.findAllByWorkflowIdOrderByOrderAsc(workflowId)
                .stream()
                .map(this::toQueryStepResponse)
                .toList();
    }

    // =====================================================================
    // INTERNAL
    // =====================================================================

    private StepsDto.StepDetailResponse loadStepDetail(UUID stepId, UUID workflowId) {
        Steps_query step = stepQueryRepo.findById(stepId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Step not found"));

        if (!step.getWorkflowId().equals(workflowId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Step not found in this workflow");
        }

        List<StepsDto.ConditionResponse> conditions = loadConditions(stepId);

        return new StepsDto.StepDetailResponse(
                step.getId().toString(),
                step.getName(),
                step.getType().name(),
                step.getOrder(),
                step.getAppKey(),
                step.getActionKey(),
                step.getConnectionId(),
                step.getConfiguration(),
                conditions,
                step.getCreatedAt(),
                step.getUpdatedAt()
        );
    }

    private List<StepsDto.ConditionResponse> loadConditions(UUID stepId) {
        return conditionRepo.findByStepId(stepId)
                .stream()
                .map(this::toConditionResponse)
                .toList();
    }

    private void verifyWorkflowOwnership(UUID userId, UUID workflowId) {
        workflowQueryRepo.findByIdAndUserId(workflowId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));
    }

    private void verifyGuestWorkflowOwnership(String guestSessionId, UUID workflowId) {
        workflowQueryRepo.findByIdAndGuestSessionId(workflowId, guestSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));
    }

    private void verifyStepBelongsToWorkflow(UUID stepId, UUID workflowId) {
        Steps_query step = stepQueryRepo.findById(stepId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Step not found"));

        if (!step.getWorkflowId().equals(workflowId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Step not found in this workflow");
        }
    }

    // =====================================================================
    // DTO MAPPERS
    // =====================================================================

    private WorkflowDto.StepResponse toQueryStepResponse(Steps_query step) {
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
