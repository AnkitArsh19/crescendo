package com.crescendo.execution.queue;

import com.crescendo.enums.WorkflowRunStatus;
import com.crescendo.logbook.workflow_run.WorkflowRun;
import com.crescendo.logbook.workflow_run.WorkflowRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Scheduled job that acts as a safety net for "stuck" workflow executions.
 *
 * <p>If the backend crashes mid-execution, or if the execution engine hangs,
 * the WorkflowRun will remain in the RUNNING state indefinitely. This reaper
 * runs every 5 minutes and sweeps any runs that have been RUNNING for more
 * than 30 minutes, forcibly failing them.
 *
 * <p>This prevents the dashboard from showing "Running..." forever and ensures
 * the audit log correctly reflects that the execution did not complete successfully.
 */
@Component
public class WorkflowRunReaper {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowRunReaper.class);
    private static final Duration TIMEOUT = Duration.ofMinutes(30);

    private final WorkflowRunRepository runRepo;

    public WorkflowRunReaper(WorkflowRunRepository runRepo) {
        this.runRepo = runRepo;
    }

    @Scheduled(fixedRate = 300_000) // Every 5 minutes
    @Transactional
    public void reapStaleRuns() {
        Instant threshold = Instant.now().minus(TIMEOUT);
        List<WorkflowRun> staleRuns = runRepo.findByStatusAndCreatedAtBefore(WorkflowRunStatus.RUNNING, threshold);

        if (staleRuns.isEmpty()) {
            return;
        }

        logger.info("[reaper] Found {} stale RUNNING workflows (older than {} mins). Marking as FAILED.",
                staleRuns.size(), TIMEOUT.toMinutes());

        for (WorkflowRun run : staleRuns) {
            run.setStatus(WorkflowRunStatus.FAILED);
            run.setErrorMessage("Execution timeout or engine crash (exceeded 30 minutes)");
            run.setCompletedAt(Instant.now());
            runRepo.save(run);
        }
    }
}
