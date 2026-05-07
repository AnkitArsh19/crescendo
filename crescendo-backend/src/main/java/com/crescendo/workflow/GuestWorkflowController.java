package com.crescendo.workflow;

import com.crescendo.workflow.workflow_command.Workflow_commandService;
import com.crescendo.workflow.workflow_command.Workflow_commandRepository;
import com.crescendo.workflow.workflow_command.Workflow_command;
import com.crescendo.workflow.workflow_query.Workflow_queryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Guest workflow endpoints under /guest/workflows.
 *
 * These endpoints are public (no JWT required). Guest identity is established
 * via the {@code X-Guest-Session} header — a client-generated UUID that the
 * frontend persists in localStorage. All ownership checks use this value.
 *
 * Guest sessions expire after 12 hours from the creation of their first workflow.
 *
 * Workflow CRUD:
 *   POST   /guest/workflows                                   — create
 *   GET    /guest/workflows                                   — list (summary)
 *   GET    /guest/workflows/{id}                              — detail with steps
 *   PATCH  /guest/workflows/{id}                              — update name/description
 *   DELETE /guest/workflows/{id}                              — soft-delete
 *
 * Step and condition endpoints are in GuestStepsController.
 */
@RestController
@RequestMapping("/guest/workflows")
public class GuestWorkflowController {

    private static final Duration GUEST_SESSION_TTL = Duration.ofHours(12);

    private final Workflow_commandService commandService;
    private final Workflow_queryService queryService;
    private final Workflow_commandRepository workflowRepo;

    public GuestWorkflowController(Workflow_commandService commandService,
                                   Workflow_queryService queryService,
                                   Workflow_commandRepository workflowRepo) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.workflowRepo = workflowRepo;
    }

    // WORKFLOW CRUD

    @PostMapping
    public ResponseEntity<WorkflowDto.WorkflowSummaryResponse> createWorkflow(
            @RequestHeader("X-Guest-Session") String guestSession,
            @Valid @RequestBody WorkflowDto.CreateWorkflowRequest req) {
        String session = validGuestSession(guestSession);
        enforceSessionExpiry(session);
        var resp = commandService.createGuestWorkflow(session, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    public ResponseEntity<List<WorkflowDto.WorkflowSummaryResponse>> listWorkflows(
            @RequestHeader("X-Guest-Session") String guestSession) {
        String session = validGuestSession(guestSession);
        enforceSessionExpiry(session);
        return ResponseEntity.ok(queryService.listGuestWorkflows(session));
    }

    @GetMapping("/{workflowId}")
    public ResponseEntity<WorkflowDto.WorkflowDetailResponse> getWorkflow(
            @PathVariable UUID workflowId,
            @RequestHeader("X-Guest-Session") String guestSession) {
        String session = validGuestSession(guestSession);
        enforceSessionExpiry(session);
        return ResponseEntity.ok(queryService.getGuestWorkflowDetail(session, workflowId));
    }

    @PatchMapping("/{workflowId}")
    public ResponseEntity<Void> updateWorkflow(
            @PathVariable UUID workflowId,
            @RequestHeader("X-Guest-Session") String guestSession,
            @Valid @RequestBody WorkflowDto.UpdateWorkflowRequest req) {
        String session = validGuestSession(guestSession);
        enforceSessionExpiry(session);
        commandService.updateGuestWorkflow(session, workflowId, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{workflowId}")
    public ResponseEntity<Void> deleteWorkflow(
            @PathVariable UUID workflowId,
            @RequestHeader("X-Guest-Session") String guestSession) {
        commandService.deleteGuestWorkflow(validGuestSession(guestSession), workflowId);
        return ResponseEntity.noContent().build();
    }

    // HELPERS

    /// Validates the guest session header is present and non-blank.
    /// The header value is a client-generated UUID stored in localStorage.
    private String validGuestSession(String guestSession) {
        if (guestSession == null || guestSession.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "X-Guest-Session header is required");
        }
        return guestSession.strip();
    }

    /**
     * Enforces the 12-hour guest session window.
     * Looks at the oldest workflow created by this guest session.
     * If it's older than 12 hours, the session is expired.
     */
    private void enforceSessionExpiry(String guestSessionId) {
        List<Workflow_command> guestWorkflows =
                workflowRepo.findAllByGuestSessionId_ValueAndDeletedAtIsNullOrderByCreatedAtDesc(guestSessionId);

        if (guestWorkflows.isEmpty()) return; // No workflows yet — session is fresh

        // Find the oldest workflow creation time
        Instant oldest = guestWorkflows.stream()
                .map(Workflow_command::getCreatedAt)
                .filter(java.util.Objects::nonNull)
                .min(Instant::compareTo)
                .orElse(null);

        if (oldest != null && Duration.between(oldest, Instant.now()).compareTo(GUEST_SESSION_TTL) > 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Guest session expired after 12 hours. Please sign up to continue using Crescendo.");
        }
    }
}
