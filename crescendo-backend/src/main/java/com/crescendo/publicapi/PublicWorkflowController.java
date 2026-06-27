package com.crescendo.publicapi;

import com.crescendo.logbook.LogbookDto;
import com.crescendo.logbook.workflow_run.WorkflowRunService;
import com.crescendo.workflow.WorkflowDto;
import com.crescendo.workflow.workflow_command.Workflow_commandService;
import com.crescendo.workflow.workflow_query.Workflow_queryService;
import jakarta.validation.Valid;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static com.crescendo.security.AuthenticatedUser.userId;
import static com.crescendo.publicapi.PublicApiScopes.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Duration;

/**
 * Public developer API for workflow management and triggering.
 * Accessible via API key (Bearer re_...) or JWT authentication.
 *
 *   POST   /api/v1/workflows                          — create a new workflow
 *   GET    /api/v1/workflows                          — list all workflows (summary)
 *   GET    /api/v1/workflows/{id}                     — workflow detail with steps
 *   PATCH  /api/v1/workflows/{id}                     — update workflow name/description
 *   DELETE /api/v1/workflows/{id}                     — soft-delete workflow
 *   POST   /api/v1/workflows/{id}/activate            — enable triggering
 *   POST   /api/v1/workflows/{id}/deactivate          — disable triggering
 *   POST   /api/v1/workflows/{id}/trigger             — trigger a workflow run
 */
@RestController
@RequestMapping("/api/v1/workflows")
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
    public ResponseEntity<WorkflowDto.WorkflowSummaryResponse> createWorkflow(
            @Valid @RequestBody WorkflowDto.CreateWorkflowRequest req,
            Authentication auth) {
        require(auth, WORKFLOW_WRITE);
        var resp = commandService.createWorkflow(userId(auth), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    public ResponseEntity<List<WorkflowDto.WorkflowSummaryResponse>> listWorkflows(Authentication auth) {
        require(auth, WORKFLOW_READ);
        return ResponseEntity.ok(queryService.listWorkflows(userId(auth)));
    }

    @GetMapping("/{workflowId}")
    public ResponseEntity<WorkflowDto.WorkflowDetailResponse> getWorkflow(
            @PathVariable UUID workflowId,
            Authentication auth) {
        require(auth, WORKFLOW_READ);
        return ResponseEntity.ok(queryService.getWorkflowDetail(userId(auth), workflowId));
    }

    @PatchMapping("/{workflowId}")
    public ResponseEntity<Void> updateWorkflow(
            @PathVariable UUID workflowId,
            @Valid @RequestBody WorkflowDto.UpdateWorkflowRequest req,
            Authentication auth) {
        require(auth, WORKFLOW_WRITE);
        commandService.updateWorkflow(userId(auth), workflowId, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{workflowId}")
    public ResponseEntity<Void> deleteWorkflow(
            @PathVariable UUID workflowId,
            Authentication auth) {
        require(auth, WORKFLOW_WRITE);
        commandService.deleteWorkflow(userId(auth), workflowId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{workflowId}/activate")
    public ResponseEntity<Void> activateWorkflow(
            @PathVariable UUID workflowId,
            Authentication auth) {
        require(auth, WORKFLOW_WRITE);
        commandService.activateWorkflow(userId(auth), workflowId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{workflowId}/deactivate")
    public ResponseEntity<Void> deactivateWorkflow(
            @PathVariable UUID workflowId,
            Authentication auth) {
        require(auth, WORKFLOW_WRITE);
        commandService.deactivateWorkflow(userId(auth), workflowId);
        return ResponseEntity.noContent().build();
    }

    // WORKFLOW TRIGGERING

    /**
     * Trigger a workflow run programmatically.
     * The workflow must be active. Returns immediately — execution is async.
     *
     * Accepts optional triggerData in the request body. If no body is provided,
     * defaults to an empty trigger payload.
     */
    @PostMapping("/{workflowId}/trigger")
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
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate Idempotency-Key for this workflow trigger");
        }
        return key;
    }

}
