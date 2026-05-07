package com.crescendo.steps;

import com.crescendo.steps.steps_command.Steps_commandService;
import com.crescendo.steps.steps_query.Steps_queryService;
import com.crescendo.workflow.WorkflowDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.crescendo.security.AuthenticatedUser.userId;

import java.util.List;
import java.util.UUID;

/**
 * Authenticated step endpoints under /workflows/{workflowId}/steps.
 *
 * All endpoints require a valid Bearer JWT — the user's ID is extracted
 * from the security context, and ownership is enforced in the service layer.
 *
 * Step CRUD:
 *   POST   /workflows/{wfId}/steps                                     — add a step
 *   GET    /workflows/{wfId}/steps                                     — list steps
 *   GET    /workflows/{wfId}/steps/{stepId}                            — step detail with conditions
 *   PATCH  /workflows/{wfId}/steps/{stepId}                            — update step config
 *   DELETE /workflows/{wfId}/steps/{stepId}                            — soft-delete step
 *   PATCH  /workflows/{wfId}/steps/{stepId}/order                      — reorder step (fractional)
 *
 * Condition CRUD:
 *   POST   /workflows/{wfId}/steps/{stepId}/conditions                 — add condition
 *   GET    /workflows/{wfId}/steps/{stepId}/conditions                 — list conditions
 *   DELETE /workflows/{wfId}/steps/{stepId}/conditions/{conditionId}   — remove condition
 */
@RestController
@RequestMapping("/workflows/{workflowId}/steps")
public class StepsController {

    private final Steps_commandService commandService;
    private final Steps_queryService queryService;

    public StepsController(Steps_commandService commandService,
                           Steps_queryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    // =====================================================================
    // STEP CRUD
    // =====================================================================

    @PostMapping
    public ResponseEntity<WorkflowDto.StepResponse> addStep(
            @PathVariable UUID workflowId,
            @Valid @RequestBody WorkflowDto.CreateStepRequest req,
            Authentication auth) {
        var resp = commandService.addStep(userId(auth), workflowId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    public ResponseEntity<List<WorkflowDto.StepResponse>> listSteps(
            @PathVariable UUID workflowId,
            Authentication auth) {
        return ResponseEntity.ok(queryService.getSteps(userId(auth), workflowId));
    }

    @GetMapping("/{stepId}")
    public ResponseEntity<StepsDto.StepDetailResponse> getStepDetail(
            @PathVariable UUID workflowId,
            @PathVariable UUID stepId,
            Authentication auth) {
        return ResponseEntity.ok(queryService.getStepDetail(userId(auth), workflowId, stepId));
    }

    @PatchMapping("/{stepId}")
    public ResponseEntity<Void> updateStep(
            @PathVariable UUID workflowId,
            @PathVariable UUID stepId,
            @Valid @RequestBody WorkflowDto.UpdateStepRequest req,
            Authentication auth) {
        commandService.updateStep(userId(auth), workflowId, stepId, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{stepId}")
    public ResponseEntity<Void> deleteStep(
            @PathVariable UUID workflowId,
            @PathVariable UUID stepId,
            Authentication auth) {
        commandService.deleteStep(userId(auth), workflowId, stepId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{stepId}/order")
    public ResponseEntity<Void> reorderStep(
            @PathVariable UUID workflowId,
            @PathVariable UUID stepId,
            @Valid @RequestBody WorkflowDto.ReorderStepRequest req,
            Authentication auth) {
        commandService.reorderStep(userId(auth), workflowId, stepId, req);
        return ResponseEntity.noContent().build();
    }

    // =====================================================================
    // CONDITION CRUD
    // =====================================================================

    @PostMapping("/{stepId}/conditions")
    public ResponseEntity<StepsDto.ConditionResponse> addCondition(
            @PathVariable UUID workflowId,
            @PathVariable UUID stepId,
            @Valid @RequestBody StepsDto.CreateConditionRequest req,
            Authentication auth) {
        var resp = commandService.addCondition(userId(auth), workflowId, stepId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/{stepId}/conditions")
    public ResponseEntity<List<StepsDto.ConditionResponse>> listConditions(
            @PathVariable UUID workflowId,
            @PathVariable UUID stepId,
            Authentication auth) {
        return ResponseEntity.ok(queryService.getConditions(userId(auth), workflowId, stepId));
    }

    @DeleteMapping("/{stepId}/conditions/{conditionId}")
    public ResponseEntity<Void> deleteCondition(
            @PathVariable UUID workflowId,
            @PathVariable UUID stepId,
            @PathVariable UUID conditionId,
            Authentication auth) {
        commandService.deleteCondition(userId(auth), workflowId, stepId, conditionId);
        return ResponseEntity.noContent().build();
    }

}
