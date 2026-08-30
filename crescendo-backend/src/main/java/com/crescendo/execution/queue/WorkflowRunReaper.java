package com.crescendo.execution.queue;

import com.crescendo.enums.StepRunStatus;
import com.crescendo.enums.WorkflowRunStatus;
import com.crescendo.logbook.step_run.StepRun;
import com.crescendo.logbook.step_run.StepRunRepository;
import com.crescendo.logbook.workflow_run.WorkflowRun;
import com.crescendo.logbook.workflow_run.WorkflowRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
 * the WorkflowRun and StepRun will remain in the RUNNING state indefinitely. This reaper
 * runs on startup and every 3 minutes to sweep stale runs, forcibly failing them.
 */
@Component
public class WorkflowRunReaper {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowRunReaper.class);
    private static final Duration TIMEOUT = Duration.ofMinutes(15);

    private final WorkflowRunRepository runRepo;
    private final StepRunRepository stepRunRepo;

    public WorkflowRunReaper(WorkflowRunRepository runRepo, StepRunRepository stepRunRepo) {
        this.runRepo = runRepo;
        this.stepRunRepo = stepRunRepo;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onStartupReap() {
        logger.info("[reaper] Running startup sweep for uncompleted runs from prior backend instances...");
        reapStaleRuns();
    }

    @Scheduled(fixedRate = 180_000) // Every 3 minutes
    @Transactional
    public void reapStaleRuns() {
        Instant threshold = Instant.now().minus(TIMEOUT);
        List<WorkflowRun> staleRuns = runRepo.findByStatusAndCreatedAtBefore(WorkflowRunStatus.RUNNING, threshold);

        for (WorkflowRun run : staleRuns) {
            run.setStatus(WorkflowRunStatus.FAILED);
            run.setErrorMessage("Execution timeout or engine crash");
            run.setCompletedAt(Instant.now());
            runRepo.save(run);

            List<StepRun> runningSteps = stepRunRepo.findAllByWorkflowRunIdAndStatus(run.getId(), StepRunStatus.RUNNING);
            for (StepRun step : runningSteps) {
                step.setStatus(StepRunStatus.FAILED);
                step.setErrorMessage("Execution terminated due to workflow timeout/restart");
                step.setCompletedAt(Instant.now());
                stepRunRepo.save(step);
            }
        }

        try {
            List<StepRun> orphanSteps = stepRunRepo.findOrphanTasks(threshold);
            for (StepRun step : orphanSteps) {
                step.setStatus(StepRunStatus.FAILED);
                step.setErrorMessage("Step execution aborted due to server restart");
                step.setCompletedAt(Instant.now());
                stepRunRepo.save(step);
            }
        } catch (Exception ignored) {}
    }
}

