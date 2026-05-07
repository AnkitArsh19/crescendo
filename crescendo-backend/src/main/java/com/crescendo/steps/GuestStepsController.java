package com.crescendo.steps;

import com.crescendo.steps.steps_command.Steps_commandService;
import com.crescendo.steps.steps_query.Steps_queryService;
import com.crescendo.workflow.WorkflowDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Guest step endpoints under /guest/workflows/{workflowId}/steps.
 *
 * These endpoints are public (no JWT required). Guest identity is established
 * via the {@code X-Guest-Session} header — a client-generated UUID that the
 * frontend persists in localStorage.
 *
 * Step CRUD:
 *   POST   /guest/workflows/{wfId}/steps                                     — add step
 *   GET    /guest/workflows/{wfId}/steps                                     — list steps
 *   GET    /guest/workflows/{wfId}/steps/{stepId}                            — step detail
 *   PATCH  /guest/workflows/{wfId}/steps/{stepId}                            — update step
 *   DELETE /guest/workflows/{wfId}/steps/{stepId}                            — delete step
 *   PATCH  /guest/workflows/{wfId}/steps/{stepId}/order                      — reorder step
 *
 * Condition CRUD:
 *   POST   /guest/workflows/{wfId}/steps/{stepId}/conditions                 — add condition
 *   GET    /guest/workflows/{wfId}/steps/{stepId}/conditions                 — list conditions
 *   DELETE /guest/workflows/{wfId}/steps/{stepId}/conditions/{conditionId}   — remove condition
 */
@RestController
@RequestMapping("/guest/workflows/{workflowId}/steps")
public class GuestStepsController {

    private final Steps_commandService commandService;
    private final Steps_queryService queryService;

    public GuestStepsController(Steps_commandService commandService,
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
            @RequestHeader("X-Guest-Session") String guestSession,
            @Valid @RequestBody WorkflowDto.CreateStepRequest req) {
        var resp = commandService.addGuestStep(validGuestSession(guestSession), workflowId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    public ResponseEntity<List<WorkflowDto.StepResponse>> listSteps(
            @PathVariable UUID workflowId,
            @RequestHeader("X-Guest-Session") String guestSession) {
        return ResponseEntity.ok(queryService.getGuestSteps(validGuestSession(guestSession), workflowId));
    }

    @GetMapping("/{stepId}")
    public ResponseEntity<StepsDto.StepDetailResponse> getStepDetail(
            @PathVariable UUID workflowId,
            @PathVariable UUID stepId,
            @RequestHeader("X-Guest-Session") String guestSession) {
        return ResponseEntity.ok(queryService.getGuestStepDetail(
                validGuestSession(guestSession), workflowId, stepId));
    }

    @PatchMapping("/{stepId}")
    public ResponseEntity<Void> updateStep(
            @PathVariable UUID workflowId,
            @PathVariable UUID stepId,
            @RequestHeader("X-Guest-Session") String guestSession,
            @Valid @RequestBody WorkflowDto.UpdateStepRequest req) {
        commandService.updateGuestStep(validGuestSession(guestSession), workflowId, stepId, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{stepId}")
    public ResponseEntity<Void> deleteStep(
            @PathVariable UUID workflowId,
            @PathVariable UUID stepId,
            @RequestHeader("X-Guest-Session") String guestSession) {
        commandService.deleteGuestStep(validGuestSession(guestSession), workflowId, stepId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{stepId}/order")
    public ResponseEntity<Void> reorderStep(
            @PathVariable UUID workflowId,
            @PathVariable UUID stepId,
            @RequestHeader("X-Guest-Session") String guestSession,
            @Valid @RequestBody WorkflowDto.ReorderStepRequest req) {
        commandService.reorderGuestStep(validGuestSession(guestSession), workflowId, stepId, req);
        return ResponseEntity.noContent().build();
    }

    // =====================================================================
    // CONDITION CRUD
    // =====================================================================

    @PostMapping("/{stepId}/conditions")
    public ResponseEntity<StepsDto.ConditionResponse> addCondition(
            @PathVariable UUID workflowId,
            @PathVariable UUID stepId,
            @RequestHeader("X-Guest-Session") String guestSession,
            @Valid @RequestBody StepsDto.CreateConditionRequest req) {
        var resp = commandService.addGuestCondition(
                validGuestSession(guestSession), workflowId, stepId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/{stepId}/conditions")
    public ResponseEntity<List<StepsDto.ConditionResponse>> listConditions(
            @PathVariable UUID workflowId,
            @PathVariable UUID stepId,
            @RequestHeader("X-Guest-Session") String guestSession) {
        return ResponseEntity.ok(queryService.getGuestConditions(
                validGuestSession(guestSession), workflowId, stepId));
    }

    @DeleteMapping("/{stepId}/conditions/{conditionId}")
    public ResponseEntity<Void> deleteCondition(
            @PathVariable UUID workflowId,
            @PathVariable UUID stepId,
            @PathVariable UUID conditionId,
            @RequestHeader("X-Guest-Session") String guestSession) {
        commandService.deleteGuestCondition(
                validGuestSession(guestSession), workflowId, stepId, conditionId);
        return ResponseEntity.noContent().build();
    }

    // =====================================================================
    // HELPERS
    // =====================================================================

    /// Validates the guest session header is present and non-blank.
    private String validGuestSession(String guestSession) {
        if (guestSession == null || guestSession.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "X-Guest-Session header is required");
        }
        return guestSession.strip();
    }
}
