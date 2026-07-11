package com.crescendo.publicapi;

import com.crescendo.logbook.LogbookDto;
import com.crescendo.logbook.workflow_run.WorkflowRunService;
import com.crescendo.workflow.WorkflowDto;
import com.crescendo.workflow.workflow_command.Workflow_commandService;
import com.crescendo.workflow.workflow_query.Workflow_queryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static com.crescendo.security.AuthenticatedUser.userId;
import static com.crescendo.publicapi.PublicApiScopes.*;

import java.util.Map;
import java.util.UUID;
import java.time.Duration;

/**
 * Public developer API for workflow management and triggering.
 * Accessible via API key (Bearer re_...) or JWT authentication.
 */
@RestController
@RequestMapping("/api/v1/workflows")
@Tag(name = "Workflows", description = "Public API for creating, managing, and triggering workflows")
public class PublicWorkflowController {

    private final Workflow_commandService commandService;
    private final Workflow_queryService queryService;
    private final WorkflowRunService runService;
    private final StringRedisTemplate redisTemplate;

    public PublicWorkflowController(Workflow_commandService commandService,
            Workflow_queryService queryService,
            WorkflowRunService runService,
            StringRedisTemplate redisTemplate) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.runService = runService;
        this.redisTemplate = redisTemplate;
    }

    // WORKFLOW CRUD

    @PostMapping
    @Operation(summary = "Create workflow", description = "Creates a new workflow. Requires workflow:write scope.")
    public ResponseEntity<WorkflowDto.WorkflowSummaryResponse> createWorkflow(
            @Valid @RequestBody WorkflowDto.CreateWorkflowRequest req,
            Authentication auth) {
        require(auth, WORKFLOW_WRITE);
        var resp = commandService.createWorkflow(userId(auth), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    @Operation(summary = "List workflows", description = "Lists workflows with optional status filter and cursor pagination. Requires workflow:read scope.")
    public ResponseEntity<WorkflowDto.WorkflowListResponse> listWorkflows(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String after,
            Authentication auth) {
        require(auth, WORKFLOW_READ);

        if (limit > 100)
            limit = 100;
        if (limit < 1)
            limit = 10;

        return ResponseEntity.ok(queryService.listWorkflowsPaginated(userId(auth), status, after, limit));
    }

    @GetMapping("/{workflowId}")
    @Operation(summary = "Get workflow details", description = "Gets metadata and step configuration for a specific workflow. Requires workflow:read scope.")
    public ResponseEntity<WorkflowDto.WorkflowDetailResponse> getWorkflow(
            @PathVariable UUID workflowId,
            Authentication auth) {
        require(auth, WORKFLOW_READ);
        return ResponseEntity.ok(queryService.getWorkflowDetail(userId(auth), workflowId));
    }

    @PatchMapping("/{workflowId}")
    @Operation(summary = "Update workflow", description = "Updates a workflow's metadata. Requires workflow:write scope.")
    public ResponseEntity<Void> updateWorkflow(
            @PathVariable UUID workflowId,
            @Valid @RequestBody WorkflowDto.UpdateWorkflowRequest req,
            Authentication auth) {
        require(auth, WORKFLOW_WRITE);
        commandService.updateWorkflow(userId(auth), workflowId, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{workflowId}")
    @Operation(summary = "Delete workflow", description = "Soft-deletes a workflow. Requires workflow:write scope.")
    public ResponseEntity<Void> deleteWorkflow(
            @PathVariable UUID workflowId,
            Authentication auth) {
        require(auth, WORKFLOW_WRITE);
        commandService.deleteWorkflow(userId(auth), workflowId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{workflowId}/activate")
    @Operation(summary = "Activate workflow", description = "Enables triggering for a workflow. Requires workflow:write scope.")
    public ResponseEntity<Void> activateWorkflow(
            @PathVariable UUID workflowId,
            Authentication auth) {
        require(auth, WORKFLOW_WRITE);
        commandService.activateWorkflow(userId(auth), workflowId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{workflowId}/deactivate")
    @Operation(summary = "Deactivate workflow", description = "Disables triggering for a workflow. Requires workflow:write scope.")
    public ResponseEntity<Void> deactivateWorkflow(
            @PathVariable UUID workflowId,
            Authentication auth) {
        require(auth, WORKFLOW_WRITE);
        commandService.deactivateWorkflow(userId(auth), workflowId);
        return ResponseEntity.noContent().build();
    }

    // WORKFLOW TRIGGERING

    @PostMapping("/{workflowId}/trigger")
    @Operation(summary = "Trigger workflow", description = "Programmatically triggers an active workflow. Returns immediately while execution happens asynchronously. Requires workflow:trigger scope.")
    public ResponseEntity<LogbookDto.WorkflowRunSummaryResponse> triggerWorkflow(
            @PathVariable UUID workflowId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) Map<String, Object> triggerData,
            Authentication auth) {
        require(auth, WORKFLOW_TRIGGER);
        String idempotencyRedisKey = claimIdempotencyKey(auth, workflowId, idempotencyKey);
        Map<String, Object> data = triggerData != null ? triggerData : Map.of();
        try {
            var req = new LogbookDto.StartWorkflowRunRequest(data);
            var resp = runService.startRun(userId(auth), workflowId, req);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(resp);
        } catch (RuntimeException e) {
            if (idempotencyRedisKey != null) {
                redisTemplate.delete(idempotencyRedisKey);
            }
            throw e;
        }
    }

    private String claimIdempotencyKey(Authentication auth, UUID workflowId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        if (idempotencyKey.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key is too long");
        }
        String key = "public-api:idempotency:trigger:" + userId(auth) + ":" + workflowId + ":" + idempotencyKey;
        Boolean claimed = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofHours(24));
        if (!Boolean.TRUE.equals(claimed)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Duplicate Idempotency-Key for this workflow trigger");
        }
        return key;
    }

}
