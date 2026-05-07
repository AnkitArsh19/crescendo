package com.crescendo.shared.infrastructure.event;

import com.crescendo.enums.WorkflowRunStatus;
import com.crescendo.enums.WorkflowStatus;
import com.crescendo.logbook.domain_event.StepRunCompletedEvent;
import com.crescendo.logbook.domain_event.WorkflowRunCompletedEvent;
import com.crescendo.logbook.domain_event.WorkflowRunStartedEvent;
import com.crescendo.workflow.workflow_query.Workflow_query;
import com.crescendo.workflow.workflow_query.Workflow_queryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import jakarta.transaction.Transactional;

import java.time.Instant;

/**
 * Listens for logbook domain events (workflow runs, step runs) and updates
 * the query-side Workflow_query projection.
 *
 * Responsibilities:
 *   - Set Workflow_query.status to RUNNING when a run starts
 *   - Set Workflow_query.lastRunAt and status when a run completes
 *   - Log step run completions for observability
 *
 * Each handler opens its own transaction because @TransactionalEventListener
 * defaults to AFTER_COMMIT, which means the originating transaction has
 * already committed and there is no active persistence context.
 */
@Component
public class LogbookEventListener {

    private static final Logger logger = LoggerFactory.getLogger(LogbookEventListener.class);

    private final Workflow_queryRepository workflowQueryRepo;

    public LogbookEventListener(Workflow_queryRepository workflowQueryRepo) {
        this.workflowQueryRepo = workflowQueryRepo;
    }

    /**
     * When a workflow run starts, mark the workflow's query projection as RUNNING.
     */
    @Transactional
    @TransactionalEventListener
    @CacheEvict(value = "workflows", allEntries = true)
    public void onWorkflowRunStarted(WorkflowRunStartedEvent event) {
        logger.info("Workflow run started: runId={}, workflowId={}, userId={}",
                event.aggregateId(), event.getWorkflowId(), event.getUserId());

        Workflow_query query = workflowQueryRepo.findById(event.getWorkflowId()).orElse(null);
        if (query != null) {
            query.setStatus(WorkflowStatus.RUNNING);
            workflowQueryRepo.save(query);
        }
    }

    /**
     * When a workflow run completes (success, failure, or cancellation),
     * update the query projection's lastRunAt and status.
     */
    @Transactional
    @TransactionalEventListener
    @CacheEvict(value = "workflows", allEntries = true)
    public void onWorkflowRunCompleted(WorkflowRunCompletedEvent event) {
        logger.info("Workflow run completed: runId={}, workflowId={}, status={}",
                event.aggregateId(), event.getWorkflowId(), event.getStatus());

        Workflow_query query = workflowQueryRepo.findById(event.getWorkflowId()).orElse(null);
        if (query != null) {
            query.setLastRunAt(Instant.now());
            query.setStatus(mapRunStatusToWorkflowStatus(event.getStatus()));
            workflowQueryRepo.save(query);
        }
    }

    /**
     * Log step run completions for observability.
     */
    @TransactionalEventListener
    @CacheEvict(value = "workflowRuns", allEntries = true)
    public void onStepRunCompleted(StepRunCompletedEvent event) {
        logger.info("Step run completed: stepRunId={}, workflowRunId={}, stepId={}, status={}",
                event.aggregateId(), event.getWorkflowRunId(), event.getStepId(), event.getStatus());
    }

    private WorkflowStatus mapRunStatusToWorkflowStatus(WorkflowRunStatus runStatus) {
        return switch (runStatus) {
            case SUCCESS -> WorkflowStatus.SUCCESS;
            case FAILED -> WorkflowStatus.FAILED;
            case CANCELLED -> WorkflowStatus.FAILED;
            case RUNNING -> WorkflowStatus.RUNNING;
            case PENDING -> WorkflowStatus.RUNNING;
        };
    }
}
