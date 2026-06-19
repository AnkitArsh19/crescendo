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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

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
 * Each handler runs synchronously in the same transaction as the publisher using @EventListener.
 * This guarantees the command (WorkflowRun) and query (Workflow_query) databases
 * stay perfectly in sync.
 */
@Component
public class LogbookEventListener {

    private static final Logger logger = LoggerFactory.getLogger(LogbookEventListener.class);

    private final Workflow_queryRepository workflowQueryRepo;
    private final CacheManager cacheManager;

    public LogbookEventListener(Workflow_queryRepository workflowQueryRepo,
                                CacheManager cacheManager) {
        this.workflowQueryRepo = workflowQueryRepo;
        this.cacheManager = cacheManager;
    }

    /**
     * When a workflow run starts, mark the workflow's query projection as RUNNING.
     */
    @Transactional
    @org.springframework.context.event.EventListener
    public void onWorkflowRunStarted(WorkflowRunStartedEvent event) {
        logger.info("Workflow run started: runId={}, workflowId={}, userId={}",
                event.aggregateId(), event.getWorkflowId(), event.getUserId());

        Workflow_query query = workflowQueryRepo.findById(event.getWorkflowId()).orElse(null);
        if (query != null) {
            query.setStatus(WorkflowStatus.RUNNING);
            workflowQueryRepo.save(query);
            evictWorkflowCaches(query);
        }
    }

    /**
     * When a workflow run completes (success, failure, or cancellation),
     * update the query projection's lastRunAt and status.
     */
    @Transactional
    @org.springframework.context.event.EventListener
    public void onWorkflowRunCompleted(WorkflowRunCompletedEvent event) {
        logger.info("Workflow run completed: runId={}, workflowId={}, status={}",
                event.aggregateId(), event.getWorkflowId(), event.getStatus());

        Workflow_query query = workflowQueryRepo.findById(event.getWorkflowId()).orElse(null);
        if (query != null) {
            query.setLastRunAt(Instant.now());
            query.setStatus(mapRunStatusToWorkflowStatus(event.getStatus()));
            workflowQueryRepo.save(query);
            evictWorkflowCaches(query);
        }
    }

    /**
     * Log step run completions for observability.
     */
    @org.springframework.context.event.EventListener
    public void onStepRunCompleted(StepRunCompletedEvent event) {
        logger.info("Step run completed: stepRunId={}, workflowRunId={}, stepId={}, status={}",
                event.aggregateId(), event.getWorkflowRunId(), event.getStepId(), event.getStatus());
    }

    private void evictWorkflowCaches(Workflow_query workflow) {
        Cache cache = cacheManager.getCache("workflows");
        if (cache == null) {
            return;
        }

        if (workflow.getUserId() != null) {
            cache.evict("detail:v2:" + workflow.getUserId() + ":" + workflow.getId());
        }
        if (workflow.getGuestSessionId() != null) {
            cache.evict("guest-detail:v2:" + workflow.getGuestSessionId() + ":" + workflow.getId());
        }
    }

    private WorkflowStatus mapRunStatusToWorkflowStatus(WorkflowRunStatus runStatus) {
        return switch (runStatus) {
            case SUCCESS -> WorkflowStatus.SUCCESS;
            case FAILED -> WorkflowStatus.FAILED;
            case CANCELLED -> WorkflowStatus.FAILED;
            case RUNNING -> WorkflowStatus.RUNNING;
            case PENDING -> WorkflowStatus.RUNNING;
            case SUSPENDED -> WorkflowStatus.RUNNING;
        };
    }
}
