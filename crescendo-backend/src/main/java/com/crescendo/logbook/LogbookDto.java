package com.crescendo.logbook;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * All request and response DTOs for logbook operations (workflow runs and step runs).
 *
 * Workflow run operations:
 *   StartWorkflowRunRequest    — trigger a new workflow run (trigger data required)
 *   WorkflowRunSummaryResponse — list-view representation (no step run detail)
 *   WorkflowRunDetailResponse  — detail-view representation (includes step runs)
 *
 * Step run operations:
 *   StartStepRunRequest        — begin executing a step within a run (internal use)
 *   CompleteStepRunRequest     — mark a step run as completed with output (internal use)
 *   FailStepRunRequest         — mark a step run as failed with error message (internal use)
 *   StepRunResponse            — read representation of a step run
 *
 * Stats:
 *   WorkflowRunStatsResponse   — aggregated run statistics for a workflow
 */
public class LogbookDto {

    // -------------------------------------------------------------------------
    // WORKFLOW RUN REQUESTS
    // -------------------------------------------------------------------------

    public record StartWorkflowRunRequest(
            @NotNull Map<String, Object> triggerData
    ) {}

    // -------------------------------------------------------------------------
    // STEP RUN REQUESTS (internal — used by execution engine)
    // -------------------------------------------------------------------------

    public record StartStepRunRequest(
            @NotNull String stepId,
            @NotNull Map<String, Object> inputData
    ) {}

    public record CompleteStepRunRequest(
            Map<String, Object> outputData
    ) {}

    public record FailStepRunRequest(
            String errorMessage
    ) {}

    // -------------------------------------------------------------------------
    // WORKFLOW RUN RESPONSES
    // -------------------------------------------------------------------------

    /// Summary view used in list endpoints — no step run detail.
    public record WorkflowRunSummaryResponse(
            String id,
            String workflowId,
            String status,
            String errorMessage,
            int totalSteps,
            int completedSteps,
            int failedSteps,
            Instant createdAt,
            Instant completedAt
    ) {}

    /// Detail view — includes ordered step runs.
    public record WorkflowRunDetailResponse(
            String id,
            String workflowId,
            String status,
            Map<String, Object> triggerData,
            String errorMessage,
            List<StepRunResponse> stepRuns,
            Instant createdAt,
            Instant completedAt
    ) {}

    // -------------------------------------------------------------------------
    // STEP RUN RESPONSES
    // -------------------------------------------------------------------------

    public record StepRunResponse(
            String id,
            String stepId,
            String status,
            Map<String, Object> inputData,
            Map<String, Object> outputData,
            String errorMessage,
            Instant createdAt,
            Instant completedAt
    ) {}

    // -------------------------------------------------------------------------
    // STATS
    // -------------------------------------------------------------------------

    public record WorkflowRunStatsResponse(
            long totalRuns,
            long successCount,
            long failedCount,
            long runningCount,
            long pendingCount
    ) {}
}
