package com.crescendo.publicapi;

import com.crescendo.logbook.LogbookDto;
import com.crescendo.logbook.step_run.StepRunQueryService;
import com.crescendo.logbook.workflow_run.WorkflowRunQueryService;
import com.crescendo.logbook.workflow_run.WorkflowRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.crescendo.security.AuthenticatedUser.userId;
import static com.crescendo.publicapi.PublicApiScopes.*;

import java.util.List;
import java.util.UUID;

/**
 * Public developer API for polling workflow run status and step-by-step logs.
 * Accessible via API key (Bearer re_...) or JWT authentication.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Workflow Runs", description = "Public API for polling execution status and logs")
public class PublicRunController {

    private final WorkflowRunService runCommandService;
    private final WorkflowRunQueryService runQueryService;
    private final StepRunQueryService stepRunQueryService;

    public PublicRunController(WorkflowRunService runCommandService,
                               WorkflowRunQueryService runQueryService,
                               StepRunQueryService stepRunQueryService) {
        this.runCommandService = runCommandService;
        this.runQueryService = runQueryService;
        this.stepRunQueryService = stepRunQueryService;
    }

    // WORKFLOW-SCOPED RUNS

    @GetMapping("/workflows/{workflowId}/runs")
    @Operation(summary = "List workflow runs", description = "Lists all execution runs for a specific workflow. Requires run:read scope.")
    public ResponseEntity<Page<LogbookDto.WorkflowRunSummaryResponse>> listRuns(
            @PathVariable UUID workflowId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        require(auth, RUN_READ);
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(runQueryService.listRunsPaged(userId(auth), workflowId, pageable));
    }

    @GetMapping("/workflows/{workflowId}/runs/{runId}")
    @Operation(summary = "Get run details", description = "Gets execution details including step results for a specific run. Requires run:read scope.")
    public ResponseEntity<LogbookDto.WorkflowRunDetailResponse> getRunDetail(
            @PathVariable UUID workflowId,
            @PathVariable UUID runId,
            Authentication auth) {
        require(auth, RUN_READ);
        return ResponseEntity.ok(runQueryService.getRunDetail(userId(auth), workflowId, runId));
    }

    @PostMapping("/workflows/{workflowId}/runs/{runId}/cancel")
    @Operation(summary = "Cancel workflow run", description = "Cancels a pending or currently executing run. Requires run:cancel scope.")
    public ResponseEntity<Void> cancelRun(
            @PathVariable UUID workflowId,
            @PathVariable UUID runId,
            Authentication auth) {
        require(auth, RUN_CANCEL);
        runCommandService.cancelRun(userId(auth), workflowId, runId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/workflows/{workflowId}/runs/stats")
    @Operation(summary = "Get run statistics", description = "Gets aggregated execution statistics for a workflow. Requires run:read scope.")
    public ResponseEntity<LogbookDto.WorkflowRunStatsResponse> getRunStats(
            @PathVariable UUID workflowId,
            Authentication auth) {
        require(auth, RUN_READ);
        return ResponseEntity.ok(runQueryService.getRunStats(userId(auth), workflowId));
    }

    // CROSS-WORKFLOW RUNS

    @GetMapping("/runs")
    @Operation(summary = "List all runs", description = "Lists execution runs across all workflows. Requires run:read scope.")
    public ResponseEntity<Page<LogbookDto.WorkflowRunSummaryResponse>> listAllRuns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        require(auth, RUN_READ);
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(runQueryService.listAllRunsPaged(userId(auth), pageable));
    }

    // STEP RUNS

    @GetMapping("/runs/{runId}/steps")
    @Operation(summary = "List step runs", description = "Lists step execution details for a specific run. Requires run:read scope.")
    public ResponseEntity<List<LogbookDto.StepRunResponse>> listStepRuns(
            @PathVariable UUID runId,
            Authentication auth) {
        require(auth, RUN_READ);
        return ResponseEntity.ok(stepRunQueryService.listStepRuns(userId(auth), runId));
    }

    @GetMapping("/runs/{runId}/steps/{stepRunId}")
    @Operation(summary = "Get step run details", description = "Gets specific step execution details. Requires run:read scope.")
    public ResponseEntity<LogbookDto.StepRunResponse> getStepRun(
            @PathVariable UUID runId,
            @PathVariable UUID stepRunId,
            Authentication auth) {
        require(auth, RUN_READ);
        return ResponseEntity.ok(stepRunQueryService.getStepRun(userId(auth), stepRunId));
    }
}
