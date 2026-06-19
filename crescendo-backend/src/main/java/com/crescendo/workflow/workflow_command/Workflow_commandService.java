package com.crescendo.workflow.workflow_command;

import com.crescendo.enums.WorkflowStatus;
import com.crescendo.security.access.AccessControlService;
import com.crescendo.shared.domain.event.DomainEventPublisher;
import com.crescendo.shared.domain.valueobject.GuestSessionId;

import com.crescendo.steps.steps_command.Steps_commandService;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import com.crescendo.workflow.WorkflowDto;
import com.crescendo.workflow.domain_event.WorkflowActivatedEvent;
import com.crescendo.workflow.domain_event.WorkflowCreatedEvent;
import com.crescendo.workflow.domain_event.WorkflowDeactivatedEvent;
import com.crescendo.workflow.domain_event.WorkflowDeletedEvent;
import com.crescendo.workflow.domain_event.WorkflowUpdatedEvent;
import com.crescendo.workflow.workflow_query.Workflow_query;
import com.crescendo.workflow.workflow_query.Workflow_queryRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Write-side service for workflow and step management.
 *
 * Every mutation:
 *   1. Validates ownership and access-tier limits
 *   2. Writes to the command database (source of truth)
 *   3. Synchronously projects changes to the query database
 *   4. Publishes a domain event (consumed by listeners for cache eviction,
 *      query-side sync, and downstream processing)
 */
@Service
@Transactional
public class Workflow_commandService {

    private final Workflow_commandRepository workflowRepo;
    private final Workflow_queryRepository workflowQueryRepo;
    private final User_commandRepository userRepo;
    private final AccessControlService accessControl;
    private final DomainEventPublisher eventPublisher;
    private final Steps_commandService stepsCommandService;
    private final WorkflowActivationValidator activationValidator;

    public Workflow_commandService(Workflow_commandRepository workflowRepo,
                                   Workflow_queryRepository workflowQueryRepo,
                                   User_commandRepository userRepo,
                                   AccessControlService accessControl,
                                   DomainEventPublisher eventPublisher,
                                   Steps_commandService stepsCommandService,
                                   WorkflowActivationValidator activationValidator) {
        this.workflowRepo = workflowRepo;
        this.workflowQueryRepo = workflowQueryRepo;
        this.userRepo = userRepo;
        this.accessControl = accessControl;
        this.eventPublisher = eventPublisher;
        this.stepsCommandService = stepsCommandService;
        this.activationValidator = activationValidator;
    }

    // WORKFLOW CRUD

    /**
     * Creates a new workflow for an authenticated user.
     * Enforces the per-tier workflow limit before persisting.
     */
    public WorkflowDto.WorkflowSummaryResponse createWorkflow(UUID userId,
                                                              WorkflowDto.CreateWorkflowRequest req) {
        accessControl.enforceWorkflowLimit(userId);

        User_command user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UUID workflowId = UUID.randomUUID();
        Workflow_command workflow = new Workflow_command(workflowId, req.name(), req.description(), user, false);
        workflowRepo.save(workflow);

        // Sync to query database
        projectWorkflowToQuery(workflow);

        eventPublisher.publish(
                new WorkflowCreatedEvent(workflowId, req.name(), userId, null));

        return toSummary(workflow, WorkflowStatus.NEVER_RUN, 0);
    }

    /**
     * Creates a new workflow for a guest session (unauthenticated user).
     * Guest limits are more restrictive — enforced through AccessControlService.
     */
    public WorkflowDto.WorkflowSummaryResponse createGuestWorkflow(String guestSessionId,
                                                                    WorkflowDto.CreateWorkflowRequest req) {
        accessControl.enforceGuestWorkflowLimit(guestSessionId);

        UUID workflowId = UUID.randomUUID();
        Workflow_command workflow = new Workflow_command(workflowId, req.name(), req.description(),
                GuestSessionId.of(guestSessionId), false);
        workflowRepo.save(workflow);

        // Sync to query database
        projectWorkflowToQuery(workflow);

        eventPublisher.publish(
                new WorkflowCreatedEvent(workflowId, req.name(), null, guestSessionId));

        return toSummary(workflow, WorkflowStatus.NEVER_RUN, 0);
    }

    /**
     * Updates a workflow's name and/or description.
     * At least one field must be provided.
     */
    public void updateWorkflow(UUID userId, UUID workflowId, WorkflowDto.UpdateWorkflowRequest req) {
        Workflow_command workflow = findOwnedWorkflow(userId, workflowId);

        if (req.name() == null && req.description() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one field must be provided");
        }

        if (req.name() != null) {
            workflow.setName(req.name());
        }
        if (req.description() != null) {
            workflow.setDescription(req.description());
        }

        // Sync to query database
        publishWorkflow(workflowId, req);
    }

    /**
     * Atomically saves the workflow graph (upserting steps and processing deletions).
     */
    public WorkflowDto.WorkflowGraphResponse saveGraph(UUID userId, UUID workflowId, WorkflowDto.WorkflowGraphRequest req) {
        Workflow_command workflow = findOwnedWorkflow(userId, workflowId);

        if (req.revision() != null && !req.revision().isBlank()) {
            if (workflow.getUpdatedAt() != null && !req.revision().equals(workflow.getUpdatedAt().toString())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Workflow has been modified by another session. Please refresh.");
            }
        }

        // We will update the updatedAt timestamp to act as a new revision
        workflow.setUpdatedAt(java.time.Instant.now());

        if (req.name() != null || req.description() != null) {
            if (req.name() != null) workflow.setName(req.name());
            if (req.description() != null) workflow.setDescription(req.description());
            publishWorkflow(workflowId, new WorkflowDto.UpdateWorkflowRequest(req.name(), req.description()));
        }

        // 1. Process deletions
        if (req.deletedStepIds() != null) {
            for (String stepIdStr : req.deletedStepIds()) {
                if (stepIdStr != null && !stepIdStr.isBlank()) {
                    try {
                        stepsCommandService.deleteStep(userId, workflowId, UUID.fromString(stepIdStr));
                    } catch (ResponseStatusException e) {
                        if (e.getStatusCode() != HttpStatus.NOT_FOUND) throw e;
                    }
                }
            }
        }

        // 2. Process upserts
        List<WorkflowDto.GraphStepResponse> savedSteps = new java.util.ArrayList<>();
        if (req.steps() != null) {
            // We'll trust the order in the list as the intended order.
            for (int i = 0; i < req.steps().size(); i++) {
                WorkflowDto.GraphStepRequest stepReq = req.steps().get(i);
                if (stepReq.backendId() != null && !stepReq.backendId().isBlank()) {
                    // Update existing
                    stepsCommandService.updateStep(userId, workflowId, UUID.fromString(stepReq.backendId()),
                            new WorkflowDto.UpdateStepRequest(
                                    stepReq.name(),
                                    stepReq.actionKey(),
                                    stepReq.appKey(),
                                    stepReq.connectionId(),
                                    stepReq.parentStepId(),
                                    stepReq.branchKey(),
                                    stepReq.configuration()
                            )
                    );
                    stepsCommandService.setStepTreePosition(userId, workflowId, UUID.fromString(stepReq.backendId()),
                            stepReq.parentStepId(), stepReq.branchKey());
                    savedSteps.add(new WorkflowDto.GraphStepResponse(stepReq.clientId(), stepReq.backendId()));
                } else {
                    // Create new
                    WorkflowDto.StepResponse created = stepsCommandService.addStep(userId, workflowId,
                            new WorkflowDto.CreateStepRequest(
                                    stepReq.name(),
                                    stepReq.type(),
                                    stepReq.actionKey(),
                                    stepReq.appKey(),
                                    stepReq.connectionId(),
                                    stepReq.parentStepId(),
                                    stepReq.branchKey(),
                                    stepReq.configuration()
                            )
                    );
                    savedSteps.add(new WorkflowDto.GraphStepResponse(stepReq.clientId(), created.id()));
                }
            }

            // 3. Reorder steps to match the requested order
            // Since we iterate through them in order, we can simply reorder them
            // wait, we can just assign new order values.
            // Since `addStep` assigns maxOrder + 1, and `updateStep` doesn't change order,
            // we should reorder them if they are not in the correct order.
            // A simple approach: use reorderStep for each step in the list.
            for (int i = 0; i < savedSteps.size(); i++) {
                WorkflowDto.GraphStepResponse saved = savedSteps.get(i);
                stepsCommandService.reorderStep(userId, workflowId, UUID.fromString(saved.backendId()),
                        new WorkflowDto.ReorderStepRequest(new java.math.BigDecimal(i + 1)));
            }
        }

        return new WorkflowDto.WorkflowGraphResponse(workflowId.toString(), workflow.getUpdatedAt().toString(), savedSteps);
    }

    /**
     * Soft-deletes a workflow and all its steps.
     * The workflow and steps remain in the database but are excluded from all queries.
     * The query-side rows are removed entirely since they serve no purpose once deleted.
     */
    public void deleteWorkflow(UUID userId, UUID workflowId) {
        Workflow_command workflow = findOwnedWorkflow(userId, workflowId);

        // Deactivate before deletion
        workflow.setActive(false);
        workflow.setDeletedAt(Instant.now());

        // Cascade soft-delete steps + conditions + query projections
        stepsCommandService.softDeleteAllForWorkflow(workflowId);

        // Remove workflow from query database
        workflowQueryRepo.deleteById(workflowId);

        eventPublisher.publish(new WorkflowDeletedEvent(workflowId));
    }

    /**
     * Activates a workflow so it can be triggered.
     * Requires STANDARD or ADMIN tier — GUEST and UNVERIFIED cannot activate.
     */
    public void activateWorkflow(UUID userId, UUID workflowId) {
        accessControl.requireCanActivateWorkflow();
        Workflow_command workflow = findOwnedWorkflow(userId, workflowId);

        if (workflow.isActive()) return; // Idempotent

        // Validate workflow structure before activation
        activationValidator.validateForActivation(workflowId);

        workflow.setActive(true);

        // Sync to query database via direct update
        workflowQueryRepo.updateIsActive(workflowId, true);

        eventPublisher.publish(new WorkflowActivatedEvent(workflowId));
    }

    /**
     * Deactivates a workflow so it stops triggering.
     */
    public void deactivateWorkflow(UUID userId, UUID workflowId) {
        Workflow_command workflow = findOwnedWorkflow(userId, workflowId);

        if (!workflow.isActive()) return; // Idempotent

        workflow.setActive(false);

        // Sync to query database via direct update
        workflowQueryRepo.updateIsActive(workflowId, false);

        eventPublisher.publish(new WorkflowDeactivatedEvent(workflowId));
    }

    // GUEST WORKFLOW OPERATIONS

    /**
     * Updates a guest-owned workflow's metadata.
     */
    public void updateGuestWorkflow(String guestSessionId, UUID workflowId,
                                    WorkflowDto.UpdateWorkflowRequest req) {
        Workflow_command workflow = findGuestOwnedWorkflow(guestSessionId, workflowId);

        if (req.name() == null && req.description() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one field must be provided");
        }

        if (req.name() != null) workflow.setName(req.name());
        if (req.description() != null) workflow.setDescription(req.description());

        publishWorkflow(workflowId, req);
    }

    /**
     * Atomically saves the guest workflow graph (upserting steps and processing deletions).
     */
    public WorkflowDto.WorkflowGraphResponse saveGuestGraph(String guestSessionId, UUID workflowId, WorkflowDto.WorkflowGraphRequest req) {
        Workflow_command workflow = findGuestOwnedWorkflow(guestSessionId, workflowId);

        if (req.name() != null || req.description() != null) {
            if (req.name() != null) workflow.setName(req.name());
            if (req.description() != null) workflow.setDescription(req.description());
            publishWorkflow(workflowId, new WorkflowDto.UpdateWorkflowRequest(req.name(), req.description()));
        }

        if (req.deletedStepIds() != null) {
            for (String stepIdStr : req.deletedStepIds()) {
                if (stepIdStr != null && !stepIdStr.isBlank()) {
                    try {
                        stepsCommandService.deleteGuestStep(guestSessionId, workflowId, UUID.fromString(stepIdStr));
                    } catch (ResponseStatusException e) {
                        if (e.getStatusCode() != HttpStatus.NOT_FOUND) throw e;
                    }
                }
            }
        }

        List<WorkflowDto.GraphStepResponse> savedSteps = new java.util.ArrayList<>();
        if (req.steps() != null) {
            for (int i = 0; i < req.steps().size(); i++) {
                WorkflowDto.GraphStepRequest stepReq = req.steps().get(i);
                if (stepReq.backendId() != null && !stepReq.backendId().isBlank()) {
                    stepsCommandService.updateGuestStep(guestSessionId, workflowId, UUID.fromString(stepReq.backendId()),
                            new WorkflowDto.UpdateStepRequest(
                                    stepReq.name(),
                                    stepReq.actionKey(),
                                    stepReq.appKey(),
                                    stepReq.connectionId(),
                                    stepReq.parentStepId(),
                                    stepReq.branchKey(),
                                    stepReq.configuration()
                            )
                    );
                    stepsCommandService.setGuestStepTreePosition(guestSessionId, workflowId, UUID.fromString(stepReq.backendId()),
                            stepReq.parentStepId(), stepReq.branchKey());
                    savedSteps.add(new WorkflowDto.GraphStepResponse(stepReq.clientId(), stepReq.backendId()));
                } else {
                    WorkflowDto.StepResponse created = stepsCommandService.addGuestStep(guestSessionId, workflowId,
                            new WorkflowDto.CreateStepRequest(
                                    stepReq.name(),
                                    stepReq.type(),
                                    stepReq.actionKey(),
                                    stepReq.appKey(),
                                    stepReq.connectionId(),
                                    stepReq.parentStepId(),
                                    stepReq.branchKey(),
                                    stepReq.configuration()
                            )
                    );
                    savedSteps.add(new WorkflowDto.GraphStepResponse(stepReq.clientId(), created.id()));
                }
            }

            for (int i = 0; i < savedSteps.size(); i++) {
                WorkflowDto.GraphStepResponse saved = savedSteps.get(i);
                stepsCommandService.reorderGuestStep(guestSessionId, workflowId, UUID.fromString(saved.backendId()),
                        new WorkflowDto.ReorderStepRequest(new java.math.BigDecimal(i + 1)));
            }
        }

        return new WorkflowDto.WorkflowGraphResponse(workflowId.toString(), req.revision(), savedSteps);
    }

    private void publishWorkflow(UUID workflowId, WorkflowDto.UpdateWorkflowRequest req) {
        Workflow_query queryWorkflow = workflowQueryRepo.findById(workflowId).orElse(null);
        if (queryWorkflow != null) {
            if (req.name() != null) queryWorkflow.setName(req.name());
            if (req.description() != null) queryWorkflow.setDescription(req.description());
            // Persist the mutation — without this the query DB name/description stays stale
            workflowQueryRepo.save(queryWorkflow);
        }

        eventPublisher.publish(new WorkflowUpdatedEvent(workflowId));
    }

    /**
     * Soft-deletes a guest-owned workflow and its steps.
     */
    public void deleteGuestWorkflow(String guestSessionId, UUID workflowId) {
        Workflow_command workflow = findGuestOwnedWorkflow(guestSessionId, workflowId);

        workflow.setActive(false);
        workflow.setDeletedAt(Instant.now());

        // Cascade soft-delete steps + conditions + query projections
        stepsCommandService.softDeleteAllForWorkflow(workflowId);

        // Remove workflow from query database
        workflowQueryRepo.deleteById(workflowId);

        eventPublisher.publish(new WorkflowDeletedEvent(workflowId));
    }

    // QUERY-SIDE PROJECTION

    /// Creates or replaces the query-side workflow row from the command-side entity.
    private void projectWorkflowToQuery(Workflow_command workflow) {
        Workflow_query q = new Workflow_query(
                workflow.getId(),
                workflow.getName(),
                workflow.getDescription(),
                workflow.getUser() != null ? workflow.getUser().getId() : null,
                workflow.getGuestSessionId(),
                workflow.isActive(),
                WorkflowStatus.NEVER_RUN,
                0
        );
        workflowQueryRepo.save(q);
    }

    // BULK OPERATIONS

    /**
     * Activates multiple workflows for the given user. Idempotent —
     * workflows that are already active remain active without error.
     * Uses the same structural validation as single activation so empty
     * or malformed workflows cannot be bulk-activated.
     */
    public void bulkActivateWorkflows(UUID userId, List<UUID> ids) {
        accessControl.requireCanActivateWorkflow();
        for (UUID workflowId : ids) {
            Workflow_command workflow = findOwnedWorkflow(userId, workflowId);
            if (!workflow.isActive()) {
                // Reuse single-workflow structural validation
                activationValidator.validateForActivation(workflowId);
                workflow.setActive(true);
                workflowQueryRepo.updateIsActive(workflowId, true);
                eventPublisher.publish(new WorkflowActivatedEvent(workflowId));
            }
        }
    }

    /**
     * Deactivates multiple workflows for the given user. Idempotent —
     * workflows that are already inactive remain inactive without error.
     */
    public void bulkDeactivateWorkflows(UUID userId, List<UUID> ids) {
        for (UUID workflowId : ids) {
            Workflow_command workflow = findOwnedWorkflow(userId, workflowId);
            if (workflow.isActive()) {
                workflow.setActive(false);
                workflowQueryRepo.updateIsActive(workflowId, false);
                eventPublisher.publish(new WorkflowDeactivatedEvent(workflowId));
            }
        }
    }

    // IMPORT SHARED WORKFLOW

    /**
     * Imports a shared workflow by creating a new workflow with the same name,
     * description, and steps. ConnectionId is left null — the user must connect
     * their own accounts after import.
     */
    public WorkflowDto.WorkflowSummaryResponse importWorkflow(UUID userId,
                                                               WorkflowDto.ImportWorkflowRequest req) {
        accessControl.enforceWorkflowLimit(userId);

        User_command user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UUID workflowId = UUID.randomUUID();
        Workflow_command workflow = new Workflow_command(workflowId, req.name(), req.description(), user, false);
        workflowRepo.save(workflow);

        // Project to query DB BEFORE adding steps so that the step_count update
        // inside addStep() finds a valid workflow_query row to update.
        projectWorkflowToQuery(workflow);

        Map<String, UUID> importedStepIdsBySourceId = new java.util.HashMap<>();
        Map<Integer, UUID> importedStepIdsByIndex = new java.util.HashMap<>();

        // Create steps first, then connect branch parents once all new IDs exist.
        if (req.steps() != null) {
            for (int i = 0; i < req.steps().size(); i++) {
                WorkflowDto.ImportStepRequest stepReq = req.steps().get(i);
                WorkflowDto.StepResponse created = stepsCommandService.addStep(userId, workflowId,
                        new WorkflowDto.CreateStepRequest(
                                stepReq.name(),
                                com.crescendo.enums.StepType.valueOf(stepReq.type()),
                                stepReq.actionKey(),
                                stepReq.appKey(),
                                null, // no connectionId — user connects later
                                null,
                                null,
                                stepReq.configuration()
                        ));
                UUID createdId = UUID.fromString(created.id());
                importedStepIdsByIndex.put(i, createdId);
                if (stepReq.sourceStepId() != null && !stepReq.sourceStepId().isBlank()) {
                    importedStepIdsBySourceId.put(stepReq.sourceStepId(), createdId);
                }
            }

            for (int i = 0; i < req.steps().size(); i++) {
                WorkflowDto.ImportStepRequest stepReq = req.steps().get(i);
                UUID parentStepId = null;
                if (stepReq.parentStepId() != null) {
                    parentStepId = importedStepIdsBySourceId.get(stepReq.parentStepId().toString());
                }
                if (parentStepId != null || stepReq.branchKey() != null) {
                    stepsCommandService.setStepTreePosition(userId, workflowId, importedStepIdsByIndex.get(i),
                            parentStepId, stepReq.branchKey());
                }
            }
        }

        // step_count is now correct because addStep() increments it on each call.
        // Re-read the final count from the command DB to be safe.
        int finalStepCount = req.steps() != null ? req.steps().size() : 0;

        eventPublisher.publish(
                new WorkflowCreatedEvent(workflowId, req.name(), userId, null));

        return toSummary(workflow, WorkflowStatus.NEVER_RUN, finalStepCount);
    }

    // OWNERSHIP VERIFICATION

    /// Validates the structural rules required to activate a workflow.
    /// Extracted so that both single and bulk activation use the same checks.

    /// Finds a non-deleted workflow and verifies the authenticated user owns it.
    private Workflow_command findOwnedWorkflow(UUID userId, UUID workflowId) {
        Workflow_command workflow = workflowRepo.findByIdAndDeletedAtIsNull(workflowId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));

        if (workflow.getUser() == null || !workflow.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this workflow");
        }
        return workflow;
    }

    /// Finds a non-deleted workflow and verifies the guest session owns it.
    private Workflow_command findGuestOwnedWorkflow(String guestSessionId, UUID workflowId) {
        Workflow_command workflow = workflowRepo.findByIdAndDeletedAtIsNull(workflowId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));

        if (workflow.getGuestSessionId() == null || !workflow.getGuestSessionId().equals(guestSessionId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this workflow");
        }
        return workflow;
    }

    // DTO MAPPERS

    private WorkflowDto.WorkflowSummaryResponse toSummary(Workflow_command w,
                                                          WorkflowStatus status,
                                                          int stepCount) {
        return new WorkflowDto.WorkflowSummaryResponse(
                w.getId().toString(),
                w.getName(),
                w.getDescription(),
                w.isActive(),
                status.name(),
                stepCount,
                w.getCreatedAt(),
                w.getUpdatedAt(),
                null // lastRunAt — set by LogbookEventListener on run completion
        );
    }
}
