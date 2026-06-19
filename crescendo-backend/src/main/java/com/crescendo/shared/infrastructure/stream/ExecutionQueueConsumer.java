package com.crescendo.shared.infrastructure.stream;

import com.crescendo.config.RedisStreamConfig;
import com.crescendo.enums.WorkflowRunStatus;
import com.crescendo.execution.engine.WorkflowExecutionEngine;
import com.crescendo.logbook.workflow_run.WorkflowRun;
import com.crescendo.logbook.workflow_run.WorkflowRunRepository;
import com.crescendo.shared.infrastructure.lock.DistributedLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis Stream consumer for the workflow execution queue.
 *
 * Consumes from {@code crescendo:queue:execution} and processes workflow run
 * requests. When a workflow run is enqueued (via {@code RedisDomainEventPublisher.enqueueExecution}),
 * this consumer picks it up, acquires a distributed lock for the workflow,
 * and transitions the run to RUNNING state.
 *
 * <p><strong>Manual ACK strategy:</strong> This consumer uses manual acknowledgement
 * (registered via {@code container.receive()} in {@link StreamConsumerRegistrar}).
 * Messages are ACK'd only after successful processing. If the consumer cannot acquire
 * the execution lock (another run in progress), the message is NOT acknowledged,
 * leaving it as a pending entry for automatic redelivery on the next poll.</p>
 *
 * The actual step-by-step execution engine is a separate concern — this consumer
 * serves as the entry point that dequeues work items and dispatches them.
 */
@Component
public class ExecutionQueueConsumer implements StreamListener<String, MapRecord<String, Object, Object>> {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionQueueConsumer.class);

    private final WorkflowRunRepository workflowRunRepo;
    private final DistributedLockService lockService;
    private final WorkflowExecutionEngine executionEngine;
    private final RedisTemplate<String, Object> redisTemplate;

    public ExecutionQueueConsumer(WorkflowRunRepository workflowRunRepo,
                                  DistributedLockService lockService,
                                  WorkflowExecutionEngine executionEngine,
                                  RedisTemplate<String, Object> redisTemplate) {
        this.workflowRunRepo = workflowRunRepo;
        this.lockService = lockService;
        this.executionEngine = executionEngine;
        this.redisTemplate = redisTemplate;
    }

    private enum ExecutionStatus { PROCESSED, RETRY, DLQ }

    @Override
    public void onMessage(MapRecord<String, Object, Object> message) {
        Map<Object, Object> raw = message.getValue();
        String workflowRunId = unquote(raw.get("workflowRunId") != null ? raw.get("workflowRunId").toString() : null);
        String workflowId = unquote(raw.get("workflowId") != null ? raw.get("workflowId").toString() : null);
        String userId = unquote(raw.get("userId") != null ? raw.get("userId").toString() : null);

        if (workflowRunId == null || workflowId == null) {
            logger.warn("[execution] Malformed execution message, missing required fields: {}", raw);
            acknowledge(message); // ACK malformed messages to prevent infinite redelivery
            return;
        }

        logger.info("[execution] Dequeued execution: workflowRunId={}, workflowId={}, userId={}",
                workflowRunId, workflowId, userId);

        String lockKey = "workflow-execution:" + workflowId;
        Optional<String> lockToken = lockService.tryLock(lockKey, 300_000); // 5 min TTL

        if (lockToken.isEmpty()) {
            // Do NOT acknowledge — the message stays as a pending entry and will be
            // redelivered automatically on the next poll cycle. This prevents message
            // loss when another run of the same workflow is in progress.
            logger.warn("[execution] Could not acquire lock for workflow {}, run {} will be redelivered",
                    workflowId, workflowRunId);
            return;
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> heartbeat = scheduler.scheduleAtFixedRate(() -> {
            try {
                lockService.extend(lockKey, lockToken.get(), 300_000); // Extend by 5 min
                logger.debug("[execution] Extended lock heartbeat for workflow {}", workflowId);
            } catch (Exception e) {
                logger.error("[execution] Failed to extend lock for workflow {}: {}", workflowId, e.getMessage());
            }
        }, 3, 3, TimeUnit.MINUTES);

        try {
            ExecutionStatus status = processExecution(workflowRunId, workflowId, userId);
            if (status == ExecutionStatus.PROCESSED) {
                acknowledge(message); // ACK only after successful processing
            } else if (status == ExecutionStatus.DLQ) {
                logger.error("[execution] Dead-lettering run {}", workflowRunId);
                // Could push to a DLQ stream here. For now, ACK to stop redelivery.
                acknowledge(message);
            } else {
                logger.info("[execution] Run {} needs retry, message will be redelivered", workflowRunId);
                // Do NOT acknowledge
            }
        } catch (Exception e) {
            logger.error("[execution] Processing failed for run {}, message will be redelivered: {}",
                    workflowRunId, e.getMessage());
            // Do NOT acknowledge — message will be redelivered for retry
        } finally {
            heartbeat.cancel(true);
            scheduler.shutdownNow();
            lockService.unlock(lockKey, lockToken.get());
        }
    }

    private ExecutionStatus processExecution(String workflowRunId, String workflowId, String userId) {
        // Retry loop: the poller saves the WorkflowRun and enqueues to Redis within
        // the same transaction. The consumer may dequeue before the DB commits.
        WorkflowRun run = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            run = workflowRunRepo.findById(UUID.fromString(workflowRunId)).orElse(null);
            if (run != null) break;
            logger.info("[execution] Workflow run {} not yet visible (attempt {}/3), waiting for DB commit...",
                    workflowRunId, attempt);
            try { Thread.sleep(2000); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ExecutionStatus.RETRY;
            }
        }
        if (run == null) {
            logger.error("[execution] Workflow run {} not found after 3 retries — giving up", workflowRunId);
            return ExecutionStatus.DLQ;
        }

        if (run.getStatus() != WorkflowRunStatus.PENDING && run.getStatus() != WorkflowRunStatus.SUSPENDED) {
            logger.info("[execution] Run {} is already {}, skipping", workflowRunId, run.getStatus());
            return ExecutionStatus.PROCESSED;
        }

        run.setStatus(WorkflowRunStatus.RUNNING);
        workflowRunRepo.save(run);

        logger.info("[execution] Run {} transitioned to RUNNING — dispatching to execution engine",
                workflowRunId);

        try {
            executionEngine.execute(run);
            return ExecutionStatus.PROCESSED;
        } catch (Exception e) {
            logger.error("[execution] Engine failed for run {}: {}", workflowRunId, e.getMessage(), e);
            run.setStatus(WorkflowRunStatus.FAILED);
            run.setErrorMessage("Engine error: " + e.getMessage());
            workflowRunRepo.save(run);
            return ExecutionStatus.DLQ;
        }
    }

    /**
     * Manually acknowledges a message in the consumer group.
     * After ACK, the message is removed from the pending entries list.
     */
    private void acknowledge(MapRecord<String, Object, Object> message) {
        try {
            redisTemplate.opsForStream().acknowledge(
                    RedisStreamConfig.STREAM_EXECUTION_QUEUE,
                    RedisStreamConfig.CONSUMER_GROUP,
                    message.getId());
        } catch (Exception e) {
            logger.error("[execution] Failed to ACK message {}: {}", message.getId(), e.getMessage());
        }
    }

    private static String unquote(String s) {
        if (s != null && s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
