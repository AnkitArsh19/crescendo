package com.crescendo.logbook.step_run;

import com.crescendo.logbook.LogbookDto;
import com.crescendo.logbook.workflow_run.WorkflowRunRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Read-side service for step run queries.
 *
 * All methods are read-only — no mutations.
 * Queries enforce user ownership through the parent workflow run.
 */
@Service
public class StepRunQueryService {

    private final StepRunRepository stepRunRepo;
    private final WorkflowRunRepository workflowRunRepo;

    public StepRunQueryService(StepRunRepository stepRunRepo,
                                WorkflowRunRepository workflowRunRepo) {
        this.stepRunRepo = stepRunRepo;
        this.workflowRunRepo = workflowRunRepo;
    }

    /**
     * Lists all step runs for a workflow run, ordered by creation time ascending.
     * Verifies the workflow run belongs to the user.
     */
    public List<LogbookDto.StepRunResponse> listStepRuns(UUID userId, UUID workflowRunId) {
        verifyRunOwnership(userId, workflowRunId);

        return stepRunRepo.findAllByWorkflowRunIdOrderByCreatedAtAsc(workflowRunId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns a single step run by ID.
     * Verifies ownership through the parent workflow run.
     */
    public LogbookDto.StepRunResponse getStepRun(UUID userId, UUID stepRunId) {
        StepRun stepRun = stepRunRepo.findById(stepRunId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Step run not found"));

        verifyRunOwnership(userId, stepRun.getWorkflowRunId());

        return toResponse(stepRun);
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    private void verifyRunOwnership(UUID userId, UUID workflowRunId) {
        workflowRunRepo.findByIdAndUserId(workflowRunId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow run not found"));
    }

    private LogbookDto.StepRunResponse toResponse(StepRun stepRun) {
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
