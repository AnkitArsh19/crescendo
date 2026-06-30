package com.crescendo.logbook;

import com.crescendo.logbook.step_run.StepRunQueryService;
import com.crescendo.logbook.workflow_run.WorkflowRunQueryService;
import com.crescendo.logbook.workflow_run.WorkflowRunService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.crescendo.security.AuthenticatedUser.userId;

import java.util.List;
import java.util.UUID;

/**
 * Authenticated logbook endpoints for workflow runs and step runs.
 *
 * All endpoints require a valid Bearer JWT — the user's ID is extracted
 * from the security context, and ownership is enforced in the service layer.
 *
 * Workflow Runs:
 *   POST   /workflows/{workflowId}/runs                       — start a new run
 *   GET    /workflows/{workflowId}/runs                       — list runs (summary)
 *   GET    /workflows/{workflowId}/runs/{runId}               — run detail with step runs
 *   POST   /workflows/{workflowId}/runs/{runId}/cancel        — cancel a pending/running run
 *   GET    /workflows/{workflowId}/runs/stats                 — aggregated run statistics
 *
 * All Runs (cross-workflow):
 *   GET    /runs                                              — list all runs across workflows
 *
 * Step Runs:
 *   GET    /workflows/{workflowId}/runs/{runId}/steps         — list step runs for a run
 *   GET    /workflows/{workflowId}/runs/{runId}/steps/{id}    — step run detail
 */
@RestController
public class LogbookController {

    private final WorkflowRunService runCommandService;
    private final WorkflowRunQueryService runQueryService;
    private final StepRunQueryService stepRunQueryService;

    public LogbookController(WorkflowRunService runCommandService,
                              WorkflowRunQueryService runQueryService,
                              StepRunQueryService stepRunQueryService) {
        this.runCommandService = runCommandService;
        this.runQueryService = runQueryService;
        this.stepRunQueryService = stepRunQueryService;
    }

    // -------------------------------------------------------------------------
    // WORKFLOW RUNS
    // -------------------------------------------------------------------------

    @PostMapping("/workflows/{workflowId}/runs")
    public ResponseEntity<LogbookDto.WorkflowRunSummaryResponse> startRun(
            @PathVariable UUID workflowId,
            @Valid @RequestBody LogbookDto.StartWorkflowRunRequest req,
            Authentication auth) {
        var resp = runCommandService.startRun(userId(auth), workflowId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

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

    // -------------------------------------------------------------------------
    // ALL RUNS (cross-workflow)
    // -------------------------------------------------------------------------

    @GetMapping("/runs")
    public ResponseEntity<Page<LogbookDto.WorkflowRunSummaryResponse>> listAllRuns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(runQueryService.listAllRunsPaged(userId(auth), pageable));
    }

    @GetMapping("/runs/stats/all")
    public ResponseEntity<LogbookDto.WorkflowRunStatsResponse> getAllRunStats(Authentication auth) {
        return ResponseEntity.ok(runQueryService.getAllRunStats(userId(auth)));
    }

    @GetMapping("/runs/search")
    public ResponseEntity<List<LogbookDto.WorkflowRunSummaryResponse>> searchRuns(
            @RequestParam String q,
            Authentication auth) {
        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(runQueryService.searchRuns(userId(auth), q.trim()));
    }

    // -------------------------------------------------------------------------
    // STEP RUNS
    // -------------------------------------------------------------------------

    @GetMapping("/workflows/{workflowId}/runs/{runId}/steps")
    public ResponseEntity<List<LogbookDto.StepRunResponse>> listStepRuns(
            @PathVariable UUID workflowId,
            @PathVariable UUID runId,
            Authentication auth) {
        return ResponseEntity.ok(stepRunQueryService.listStepRuns(userId(auth), runId));
    }

    @GetMapping("/workflows/{workflowId}/runs/{runId}/steps/{stepRunId}")
    public ResponseEntity<LogbookDto.StepRunResponse> getStepRun(
            @PathVariable UUID workflowId,
            @PathVariable UUID runId,
            @PathVariable UUID stepRunId,
            Authentication auth) {
        return ResponseEntity.ok(stepRunQueryService.getStepRun(userId(auth), stepRunId));
    }

}
