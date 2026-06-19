package com.crescendo.execution.queue;

import com.crescendo.config.RedisStreamConfig;

import com.crescendo.logbook.outbox.OutboxEvent;
import com.crescendo.logbook.outbox.OutboxEventRepository;
import com.crescendo.logbook.workflow_run.WorkflowRun;
import com.crescendo.logbook.workflow_run.WorkflowRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Component
public class WorkflowResumeReaper {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowResumeReaper.class);

    private final WorkflowRunRepository runRepo;
    private final OutboxEventRepository outboxEventRepository;

    public WorkflowResumeReaper(WorkflowRunRepository runRepo, OutboxEventRepository outboxEventRepository) {
        this.runRepo = runRepo;
        this.outboxEventRepository = outboxEventRepository;
    }

    /**
     * Runs every 10 seconds.
     * Finds suspended workflows where resumeAt has elapsed and enqueues them for execution.
     */
    @Scheduled(fixedRate = 10000)
    public void resumeSuspendedWorkflows() {
        // Find all suspended runs where resumeAt <= now
        List<WorkflowRun> readyToResume = runRepo.findReadyToResumeWorkflows(Instant.now());

        if (readyToResume.isEmpty()) {
            return;
        }

        logger.info("[reaper] Found {} suspended workflow runs ready to resume", readyToResume.size());

        for (WorkflowRun run : readyToResume) {
            try {
                // Enqueue via outbox pattern
                OutboxEvent outboxEvent = new OutboxEvent(
                        UUID.randomUUID(),
                        RedisStreamConfig.STREAM_EXECUTION_QUEUE,
                        buildExecutionPayload(run.getId(), run.getWorkflowId(), run.getUserId())
                );
                outboxEventRepository.save(outboxEvent);
                
                // Technically we could leave it SUSPENDED until the engine picks it up,
                // or we could change it to PENDING. Leaving it SUSPENDED avoids picking it up
                // multiple times if the queue is slow, because we clear resumeAt.
                run.setResumeAt(null); // Prevent picking up again
                runRepo.save(run);
                
                logger.debug("[reaper] Enqueued run {} for resumption", run.getId());
            } catch (Exception e) {
                logger.error("[reaper] Failed to enqueue run {}", run.getId(), e);
            }
        }
    }

    private HashMap<String, Object> buildExecutionPayload(UUID runId, UUID workflowId, UUID userId) {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("workflowRunId", runId.toString());
        payload.put("workflowId", workflowId.toString());
        payload.put("userId", userId != null ? userId.toString() : null);
        return payload;
    }
}
