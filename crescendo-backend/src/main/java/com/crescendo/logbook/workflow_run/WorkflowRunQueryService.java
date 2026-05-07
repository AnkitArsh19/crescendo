package com.crescendo.logbook.workflow_run;

import com.crescendo.enums.StepRunStatus;
import com.crescendo.enums.WorkflowRunStatus;
import com.crescendo.logbook.LogbookDto;
import com.crescendo.logbook.step_run.StepRun;
import com.crescendo.logbook.step_run.StepRunRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Read-side service for workflow run queries.
 *
 * All methods are read-only — no mutations.
 * Queries enforce user ownership to prevent data leakage.
 */
@Service
public class WorkflowRunQueryService {

    private final WorkflowRunRepository runRepo;
    private final StepRunRepository stepRunRepo;

    public WorkflowRunQueryService(WorkflowRunRepository runRepo,
                                    StepRunRepository stepRunRepo) {
        this.runRepo = runRepo;
        this.stepRunRepo = stepRunRepo;
    }

    /**
     * Lists all runs for a workflow (owned by the user), newest first.
     */
    public List<LogbookDto.WorkflowRunSummaryResponse> listRuns(UUID userId, UUID workflowId) {
        return runRepo.findAllByWorkflowIdAndUserIdOrderByCreatedAtDesc(workflowId, userId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * Lists runs for a workflow with pagination.
     */
    public Page<LogbookDto.WorkflowRunSummaryResponse> listRunsPaged(UUID userId, UUID workflowId,
                                                                      Pageable pageable) {
        return runRepo.findAllByWorkflowIdAndUserId(workflowId, userId, pageable)
                .map(this::toSummary);
    }

    /**
     * Lists all runs across the user's workflows, newest first.
     */
    public List<LogbookDto.WorkflowRunSummaryResponse> listAllRuns(UUID userId) {
        return runRepo.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * Lists all runs across the user's workflows with pagination.
     */
    public Page<LogbookDto.WorkflowRunSummaryResponse> listAllRunsPaged(UUID userId, Pageable pageable) {
        return runRepo.findAllByUserId(userId, pageable)
                .map(this::toSummary);
    }

    /**
     * Returns a single workflow run with all its step runs.
     * Verifies user ownership.
     */
    public LogbookDto.WorkflowRunDetailResponse getRunDetail(UUID userId, UUID workflowId, UUID runId) {
        WorkflowRun run = runRepo.findByIdAndUserId(runId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow run not found"));

        if (!run.getWorkflowId().equals(workflowId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow run not found");
        }

        List<LogbookDto.StepRunResponse> stepRuns = stepRunRepo
                .findAllByWorkflowRunIdOrderByCreatedAtAsc(runId)
                .stream()
                .map(this::toStepRunResponse)
                .toList();

        return toDetail(run, stepRuns);
    }

    /**
     * Returns aggregated run statistics for a workflow owned by the user.
     */
    public LogbookDto.WorkflowRunStatsResponse getRunStats(UUID userId, UUID workflowId) {
        long total = runRepo.countByWorkflowIdAndUserId(workflowId, userId);
        long success = runRepo.countByWorkflowIdAndUserIdAndStatus(workflowId, userId, WorkflowRunStatus.SUCCESS);
        long failed = runRepo.countByWorkflowIdAndUserIdAndStatus(workflowId, userId, WorkflowRunStatus.FAILED);
        long running = runRepo.countByWorkflowIdAndUserIdAndStatus(workflowId, userId, WorkflowRunStatus.RUNNING);
        long pending = runRepo.countByWorkflowIdAndUserIdAndStatus(workflowId, userId, WorkflowRunStatus.PENDING);

        return new LogbookDto.WorkflowRunStatsResponse(total, success, failed, running, pending);
    }

    // -------------------------------------------------------------------------
    // DTO MAPPERS
    // -------------------------------------------------------------------------

    private LogbookDto.WorkflowRunSummaryResponse toSummary(WorkflowRun run) {
        long totalSteps = stepRunRepo.countByWorkflowRunId(run.getId());
        long completedSteps = stepRunRepo.countByWorkflowRunIdAndStatus(run.getId(), StepRunStatus.SUCCESS);
        long failedSteps = stepRunRepo.countByWorkflowRunIdAndStatus(run.getId(), StepRunStatus.FAILED);

        return new LogbookDto.WorkflowRunSummaryResponse(
                run.getId().toString(),
                run.getWorkflowId().toString(),
                run.getStatus().name(),
                run.getErrorMessage(),
                (int) totalSteps,
                (int) completedSteps,
                (int) failedSteps,
                run.getCreatedAt(),
                run.getCompletedAt()
        );
    }

    private LogbookDto.WorkflowRunDetailResponse toDetail(WorkflowRun run,
                                                           List<LogbookDto.StepRunResponse> stepRuns) {
        return new LogbookDto.WorkflowRunDetailResponse(
                run.getId().toString(),
                run.getWorkflowId().toString(),
                run.getStatus().name(),
                run.getTriggerData(),
                run.getErrorMessage(),
                stepRuns,
                run.getCreatedAt(),
                run.getCompletedAt()
        );
    }

    private LogbookDto.StepRunResponse toStepRunResponse(StepRun stepRun) {
        return new LogbookDto.StepRunResponse(
                stepRun.getId().toString(),
                stepRun.getStepId().toString(),
                stepRun.getStatus().name(),
                stepRun.getInputData(),
                stepRun.getOutputData(),
                stepRun.getErrorMessage(),
                stepRun.getCreatedAt(),
                stepRun.getCompletedAt()
        );
    }
}
