package com.crescendo.publicapi;

import com.crescendo.logbook.LogbookDto;
import com.crescendo.logbook.step_run.StepRunQueryService;
import com.crescendo.logbook.workflow_run.WorkflowRunQueryService;
import com.crescendo.logbook.workflow_run.WorkflowRunService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.crescendo.security.AuthenticatedUser.userId;

import java.util.List;
import java.util.UUID;

/**
 * Public developer API for polling workflow run status and step-by-step logs.
 * Accessible via API key (Bearer re_...) or JWT authentication.
 *
 * Workflow-scoped runs:
 *   GET    /api/v1/workflows/{workflowId}/runs                — list runs for a workflow
 *   GET    /api/v1/workflows/{workflowId}/runs/{runId}        — run detail with step runs
 *   POST   /api/v1/workflows/{workflowId}/runs/{runId}/cancel — cancel a pending/running run
 *   GET    /api/v1/workflows/{workflowId}/runs/stats          — aggregated run statistics
 *
 * Cross-workflow runs:
 *   GET    /api/v1/runs                                       — list all runs across workflows
 *
 * Step runs:
 *   GET    /api/v1/runs/{runId}/steps                         — list step runs for a run
 *   GET    /api/v1/runs/{runId}/steps/{stepRunId}             — step run detail
 */
@RestController
@RequestMapping("/api/v1")
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
    public ResponseEntity<Page<LogbookDto.WorkflowRunSummaryResponse>> listRuns(
            @PathVariable UUID workflowId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(runQueryService.listRunsPaged(userId(auth), workflowId, pageable));
    }

    @GetMapping("/workflows/{workflowId}/runs/{runId}")
    public ResponseEntity<LogbookDto.WorkflowRunDetailResponse> getRunDetail(
            @PathVariable UUID workflowId,
            @PathVariable UUID runId,
            Authentication auth) {
        return ResponseEntity.ok(runQueryService.getRunDetail(userId(auth), workflowId, runId));
    }

    @PostMapping("/workflows/{workflowId}/runs/{runId}/cancel")
    public ResponseEntity<Void> cancelRun(
            @PathVariable UUID workflowId,
            @PathVariable UUID runId,
            Authentication auth) {
        runCommandService.cancelRun(userId(auth), workflowId, runId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/workflows/{workflowId}/runs/stats")
    public ResponseEntity<LogbookDto.WorkflowRunStatsResponse> getRunStats(
            @PathVariable UUID workflowId,
            Authentication auth) {
        return ResponseEntity.ok(runQueryService.getRunStats(userId(auth), workflowId));
    }

    // CROSS-WORKFLOW RUNS

    @GetMapping("/runs")
    public ResponseEntity<Page<LogbookDto.WorkflowRunSummaryResponse>> listAllRuns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(runQueryService.listAllRunsPaged(userId(auth), pageable));
    }

    // STEP RUNS

    @GetMapping("/runs/{runId}/steps")
    public ResponseEntity<List<LogbookDto.StepRunResponse>> listStepRuns(
            @PathVariable UUID runId,
            Authentication auth) {
        return ResponseEntity.ok(stepRunQueryService.listStepRuns(userId(auth), runId));
    }

    @GetMapping("/runs/{runId}/steps/{stepRunId}")
    public ResponseEntity<LogbookDto.StepRunResponse> getStepRun(
            @PathVariable UUID runId,
            @PathVariable UUID stepRunId,
            Authentication auth) {
        return ResponseEntity.ok(stepRunQueryService.getStepRun(userId(auth), stepRunId));
    }

}
