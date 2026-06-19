package com.crescendo.workflow;

import com.crescendo.workflow.workflow_command.Workflow_commandService;
import com.crescendo.workflow.workflow_query.Workflow_queryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.crescendo.security.AuthenticatedUser.userId;

import java.util.List;
import java.util.UUID;

/**
 * Authenticated workflow endpoints under /workflows.
 *
 * All endpoints require a valid Bearer JWT — the user's ID is extracted
 * from the security context, and ownership is enforced in the service layer.
 *
 * Workflow CRUD:
 *   POST   /workflows                                   — create a new workflow
 *   GET    /workflows                                   — list all workflows (summary)
 *   GET    /workflows/{id}                              — workflow detail with steps
 *   PATCH  /workflows/{id}                              — update name / description
 *   DELETE /workflows/{id}                              — soft-delete workflow + steps
 *   POST   /workflows/{id}/activate                     — enable triggering
 *   POST   /workflows/{id}/deactivate                   — disable triggering
 *
 * Step and condition endpoints are in StepsController.
 */
@RestController
@RequestMapping("/workflows")
public class WorkflowController {

    private final Workflow_commandService commandService;
    private final Workflow_queryService queryService;

    public WorkflowController(Workflow_commandService commandService,
                              Workflow_queryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    // WORKFLOW CRUD

    @PostMapping
    public ResponseEntity<WorkflowDto.WorkflowSummaryResponse> createWorkflow(
            @Valid @RequestBody WorkflowDto.CreateWorkflowRequest req,
            Authentication auth) {
        var resp = commandService.createWorkflow(userId(auth), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    public ResponseEntity<List<WorkflowDto.WorkflowSummaryResponse>> listWorkflows(Authentication auth) {
        return ResponseEntity.ok(queryService.listWorkflows(userId(auth)));
    }

    @GetMapping("/{workflowId}")
    public ResponseEntity<WorkflowDto.WorkflowDetailResponse> getWorkflow(
            @PathVariable UUID workflowId,
            Authentication auth) {
        return ResponseEntity.ok(queryService.getWorkflowDetail(userId(auth), workflowId));
    }

    @PatchMapping("/{workflowId}")
    public ResponseEntity<Void> updateWorkflow(
            @PathVariable UUID workflowId,
            @Valid @RequestBody WorkflowDto.UpdateWorkflowRequest req,
            Authentication auth) {
        commandService.updateWorkflow(userId(auth), workflowId, req);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{workflowId}/graph")
    public ResponseEntity<WorkflowDto.WorkflowGraphResponse> saveGraph(
            @PathVariable UUID workflowId,
            @Valid @RequestBody WorkflowDto.WorkflowGraphRequest req,
            Authentication auth) {
        var resp = commandService.saveGraph(userId(auth), workflowId, req);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{workflowId}")
    public ResponseEntity<Void> deleteWorkflow(
            @PathVariable UUID workflowId,
            Authentication auth) {
        commandService.deleteWorkflow(userId(auth), workflowId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{workflowId}/activate")
    public ResponseEntity<Void> activateWorkflow(
            @PathVariable UUID workflowId,
            Authentication auth) {
        commandService.activateWorkflow(userId(auth), workflowId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{workflowId}/deactivate")
    public ResponseEntity<Void> deactivateWorkflow(
            @PathVariable UUID workflowId,
            Authentication auth) {
        commandService.deactivateWorkflow(userId(auth), workflowId);
        return ResponseEntity.noContent().build();
    }

    // BULK OPERATIONS

    @PostMapping("/bulk/activate")
    public ResponseEntity<Void> bulkActivate(
            @Valid @RequestBody WorkflowDto.BulkWorkflowRequest req,
            Authentication auth) {
        List<UUID> ids = req.ids().stream().map(UUID::fromString).toList();
        commandService.bulkActivateWorkflows(userId(auth), ids);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk/deactivate")
    public ResponseEntity<Void> bulkDeactivate(
            @Valid @RequestBody WorkflowDto.BulkWorkflowRequest req,
            Authentication auth) {
        List<UUID> ids = req.ids().stream().map(UUID::fromString).toList();
        commandService.bulkDeactivateWorkflows(userId(auth), ids);
        return ResponseEntity.noContent().build();
    }

    // IMPORT SHARED WORKFLOW

    @PostMapping("/import")
    public ResponseEntity<WorkflowDto.WorkflowSummaryResponse> importWorkflow(
            @Valid @RequestBody WorkflowDto.ImportWorkflowRequest req,
            Authentication auth) {
        var resp = commandService.importWorkflow(userId(auth), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

}
