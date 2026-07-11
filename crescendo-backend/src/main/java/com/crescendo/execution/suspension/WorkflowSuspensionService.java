package com.crescendo.execution.suspension;

import com.crescendo.config.RedisStreamConfig;
import com.crescendo.logbook.outbox.OutboxEvent;
import com.crescendo.logbook.outbox.OutboxEventRepository;
import com.crescendo.logbook.workflow_run.WorkflowRun;
import com.crescendo.logbook.workflow_run.WorkflowRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class WorkflowSuspensionService {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowSuspensionService.class);

    private final WorkflowSuspensionRepository suspensionRepo;
    private final WorkflowRunRepository runRepo;
    private final OutboxEventRepository outboxRepo;

    public WorkflowSuspensionService(
            WorkflowSuspensionRepository suspensionRepo,
            WorkflowRunRepository runRepo,
            OutboxEventRepository outboxRepo) {
        this.suspensionRepo = suspensionRepo;
        this.runRepo = runRepo;
        this.outboxRepo = outboxRepo;
    }

    /**
     * Pauses a workflow run, persisting the suspension state so it can be resumed
     * when an external event matching correlationKey occurs.
     */
    public void suspend(UUID runId, UUID stepId, String correlationKey, String resumeToken, Instant timeoutAt) {
        // Prevent duplicate correlation keys
        if (suspensionRepo.findByCorrelationKeyAndStatus(correlationKey, WorkflowSuspension.SuspensionStatus.WAITING)
                .isPresent()) {
            throw new IllegalArgumentException(
                    "A waiting suspension with correlation key " + correlationKey + " already exists.");
        }

        WorkflowSuspension suspension = new WorkflowSuspension(
                UUID.randomUUID(), runId, stepId, correlationKey, resumeToken, timeoutAt);
        suspensionRepo.save(suspension);
        logger.info("[suspension] Run {} suspended at step {}, waiting for correlation key '{}'", runId, stepId,
                correlationKey);
    }

    public Optional<WorkflowSuspension> getWaitingSuspension(String correlationKey) {
        return suspensionRepo.findByCorrelationKeyAndStatus(
                correlationKey, WorkflowSuspension.SuspensionStatus.WAITING);
    }

    /**
     * Resumes a suspended workflow. The provided payload is injected into the
     * execution state as the output of the step that requested the suspension.
     */
    public boolean resume(String correlationKey, Map<String, Object> payload) {
        Optional<WorkflowSuspension> opt = suspensionRepo.findByCorrelationKeyAndStatus(
                correlationKey, WorkflowSuspension.SuspensionStatus.WAITING);

        if (opt.isEmpty()) {
            logger.warn("[suspension] No waiting suspension found for correlation key '{}'", correlationKey);
            return false;
        }

        WorkflowSuspension suspension = opt.get();
        doResume(suspension, payload, WorkflowSuspension.SuspensionStatus.RESUMED);
        return true;
    }

    /**
     * Scheduled job to find suspensions whose timeout has elapsed,
     * resuming them with a timeout payload.
     */
    @Scheduled(fixedRate = 60000)
    public void checkTimeouts() {
        List<WorkflowSuspension> timedOut = suspensionRepo.findByStatusAndTimeoutAtBefore(
                WorkflowSuspension.SuspensionStatus.WAITING, Instant.now());

        for (WorkflowSuspension suspension : timedOut) {
            Map<String, Object> timeoutPayload = Map.of("timeout", true);
            doResume(suspension, timeoutPayload, WorkflowSuspension.SuspensionStatus.TIMED_OUT);
        }
    }

    private void doResume(WorkflowSuspension suspension, Map<String, Object> payload,
            WorkflowSuspension.SuspensionStatus endStatus) {
        suspension.setStatus(endStatus);
        suspension.setResumedAt(Instant.now());
        suspensionRepo.save(suspension);

        Optional<WorkflowRun> runOpt = runRepo.findById(suspension.getRunId());
        if (runOpt.isEmpty()) {
            logger.error("[suspension] WorkflowRun {} not found for suspension {}", suspension.getRunId(),
                    suspension.getId());
            return;
        }

        WorkflowRun run = runOpt.get();

        // Ensure we only resume if the run actually expects it
        if (!suspension.getResumeToken().equals(run.getResumeToken())) {
            logger.warn("[suspension] Resume token mismatch for run {}. Expected {}, got {}. Ignoring.",
                    run.getId(), run.getResumeToken(), suspension.getResumeToken());
            return;
        }

        // Inject payload into step output
        try {
            Map<String, Object> state = run.getExecutionState() != null
                    ? new HashMap<>(run.getExecutionState())
                    : new HashMap<>();

            if (!state.isEmpty()) {
                Integer maxOrder = state.keySet().stream()
                        .map(k -> {
                            try {
                                return Integer.parseInt(k);
                            } catch (Exception e) {
                                return -1;
                            }
                        })
                        .max(Integer::compareTo)
                        .orElse(0);

                String maxOrderStr = maxOrder.toString();
                Object rawStepOutput = state.getOrDefault(maxOrderStr, new HashMap<>());
                Map<String, Object> stepOutput = new HashMap<>();
                if (rawStepOutput instanceof Map<?, ?> m) {
                    for (Map.Entry<?, ?> e : m.entrySet()) {
                        stepOutput.put(String.valueOf(e.getKey()), e.getValue());
                    }
                }
                stepOutput.putAll(payload);
                state.put(maxOrderStr, stepOutput);
                run.setExecutionState(state);
            }
        } catch (Exception e) {
            logger.error("[suspension] Failed to modify execution state for run {}", run.getId(), e);
        }

        run.setResumeAt(null); // Clear resumeAt so it gets picked up immediately
        run.setResumeToken(null);
        runRepo.save(run);

        // Enqueue
        HashMap<String, Object> queuePayload = new HashMap<>();
        queuePayload.put("workflowRunId", run.getId().toString());
        queuePayload.put("workflowId", run.getWorkflowId().toString());
        queuePayload.put("userId", run.getUserId() != null ? run.getUserId().toString() : null);

        outboxRepo.save(new OutboxEvent(
                UUID.randomUUID(), RedisStreamConfig.STREAM_EXECUTION_QUEUE, queuePayload));

        logger.info("[suspension] Run {} resumed via correlation key {}", run.getId(), suspension.getCorrelationKey());
    }
}
