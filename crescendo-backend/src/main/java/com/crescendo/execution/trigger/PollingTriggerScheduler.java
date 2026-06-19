package com.crescendo.execution.trigger;

import com.crescendo.connections.connections_command.Connections_command;
import com.crescendo.connections.connections_command.Connections_commandRepository;
import com.crescendo.connections.oauth.OAuthTokenRefreshService;
import com.crescendo.enums.StepType;
import com.crescendo.enums.WorkflowRunStatus;
import com.crescendo.logbook.domain_event.WorkflowRunStartedEvent;
import com.crescendo.logbook.workflow_run.WorkflowRun;
import com.crescendo.logbook.workflow_run.WorkflowRunRepository;
import com.crescendo.shared.domain.event.DomainEventPublisher;
import com.crescendo.logbook.outbox.OutboxEvent;
import com.crescendo.logbook.outbox.OutboxEventRepository;
import com.crescendo.config.RedisStreamConfig;
import com.crescendo.steps.steps_command.Steps_command;
import com.crescendo.steps.steps_command.Steps_commandRepository;
import com.crescendo.workflow.workflow_command.Workflow_command;
import com.crescendo.workflow.workflow_command.Workflow_commandRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Scheduled task that polls for trigger events on active workflows.
 *
 * Runs every 2 minutes. For each active workflow, it finds the TRIGGER step,
 * checks if a registered {@link TriggerPoller} supports it, and if so, polls
 * the external API for new events. When new events are found, it enqueues
 * a workflow run.
 *
 * Last-poll timestamps are stored in Redis to survive restarts.
 */
@Component
public class PollingTriggerScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PollingTriggerScheduler.class);
    private static final String LAST_POLL_KEY_PREFIX = "polling:lastPoll:";
    private static final Duration DEFAULT_LOOKBACK = Duration.ofMinutes(30);

    private final List<TriggerPoller> pollers;
    private final Workflow_commandRepository workflowRepo;
    private final Steps_commandRepository stepsRepo;
    private final Connections_commandRepository connectionsRepo;
    private final OAuthTokenRefreshService tokenService;
    private final WorkflowRunRepository runRepo;
    private final DomainEventPublisher eventPublisher;
    private final OutboxEventRepository outboxRepo;
    private final StringRedisTemplate redisTemplate;

    public PollingTriggerScheduler(List<TriggerPoller> pollers,
                                    Workflow_commandRepository workflowRepo,
                                    Steps_commandRepository stepsRepo,
                                    Connections_commandRepository connectionsRepo,
                                    OAuthTokenRefreshService tokenService,
                                    WorkflowRunRepository runRepo,
                                    DomainEventPublisher eventPublisher,
                                    OutboxEventRepository outboxRepo,
                                    StringRedisTemplate redisTemplate) {
        this.pollers = pollers;
        this.workflowRepo = workflowRepo;
        this.stepsRepo = stepsRepo;
        this.connectionsRepo = connectionsRepo;
        this.tokenService = tokenService;
        this.runRepo = runRepo;
        this.eventPublisher = eventPublisher;
        this.outboxRepo = outboxRepo;
        this.redisTemplate = redisTemplate;
    }

    /**
     * On startup, do NOT clear poll cursors. The cursor in Redis is the source of truth
     * for "what has already been processed". Clearing it on every restart causes the same
     * emails to be re-detected and re-executed after every backend restart.
     *
     * If you need to force a re-scan (e.g. for debugging), call resetPollCursor() manually
     * or delete the Redis key: polling:lastPoll:<stepId>
     */
    @PostConstruct
    public void logStartupCursorState() {
        try {
            var keys = redisTemplate.keys(LAST_POLL_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                logger.info("[poller] {} poll cursor(s) found in Redis — resuming from last known positions", keys.size());
            } else {
                logger.info("[poller] No poll cursors found — first poll will look back {} min", DEFAULT_LOOKBACK.toMinutes());
            }
        } catch (Exception e) {
            logger.warn("[poller] Could not read poll cursor state on startup: {}", e.getMessage());
        }
    }

    /** Resets the poll cursor for a specific step (useful for debugging). */
    public void resetPollCursor(UUID stepId) {
        redisTemplate.delete(LAST_POLL_KEY_PREFIX + stepId);
        logger.info("[poller] Reset poll cursor for step {}", stepId);
    }

    /**
     * Runs every 2 minutes. Finds active workflows with polling triggers
     * and checks for new events.
     */
    @Scheduled(fixedRate = 120_000, initialDelay = 30_000)
    @Transactional
    public void pollActiveWorkflows() {
        logger.debug("[poller] Polling cycle started");

        List<Workflow_command> activeWorkflows = workflowRepo.findAll().stream()
                .filter(w -> w.isActive() && w.getDeletedAt() == null && w.getUser() != null)
                .toList();

        if (activeWorkflows.isEmpty()) {
            logger.debug("[poller] No active workflows to poll");
            return;
        }

        logger.info("[poller] Checking {} active workflow(s) for polling triggers", activeWorkflows.size());

        int triggered = 0;
        for (Workflow_command workflow : activeWorkflows) {
            try {
                triggered += pollWorkflow(workflow);
            } catch (Exception e) {
                logger.error("[poller] Error polling workflow {}: {}",
                        workflow.getId(), e.getMessage(), e);
            }
        }

        if (triggered > 0) {
            logger.info("[poller] Polling cycle complete — triggered {} run(s)", triggered);
        } else {
            logger.debug("[poller] Polling cycle complete — no new events found");
        }
    }

    private int pollWorkflow(Workflow_command workflow) {
        UUID workflowId = workflow.getId();
        UUID userId = workflow.getUser().getId();

        // Find the trigger step
        List<Steps_command> triggerSteps = stepsRepo.findActiveByWorkflowId(workflowId).stream()
                .filter(s -> s.getType() == StepType.TRIGGER)
                .toList();

        if (triggerSteps.isEmpty()) {
            return 0;
        }

        int triggered = 0;
        for (Steps_command triggerStep : triggerSteps) {
            String appKey = triggerStep.getAppKey();
            String triggerKey = triggerStep.getActionKey(); // triggers use actionKey field

            // Find a poller that supports this trigger
            TriggerPoller poller = pollers.stream()
                    .filter(p -> p.supports(appKey, triggerKey))
                    .findFirst()
                    .orElse(null);

            if (poller == null) {
                // This trigger doesn't support polling (webhook-only) — skip
                continue;
            }

            try {
                UUID connectionId = triggerStep.getConnectionId();
                Map<String, Object> credentials = Map.of();
                if (connectionId != null) {
                    Connections_command connection = connectionsRepo.findByIdAndUser_Id(connectionId, userId)
                            .orElse(null);
                    if (connection == null) {
                        logger.warn("[poller] Connection {} not found for user {}", connectionId, userId);
                        continue;
                    }
                    credentials = tokenService.getValidCredentials(connection);
                }

                // Get the last poll time from Redis
                Instant lastPollTime = getLastPollTime(triggerStep.getId());

                // Poll for new events
                Map<String, Object> stepConfig = new HashMap<>(triggerStep.getConfiguration() != null
                        ? triggerStep.getConfiguration()
                        : Map.of());
                stepConfig.put("appKey", appKey);
                stepConfig.put("triggerKey", triggerKey);

                logger.info("[poller] Polling {}:{} — stepId={}, connectionId={}, config={}, lastPoll={}",
                        appKey, triggerKey, triggerStep.getId(), triggerStep.getConnectionId(), stepConfig, lastPollTime);

                List<Map<String, Object>> newEvents = poller.poll(credentials, stepConfig, lastPollTime);

                Instant nextPollTime = lastPollTime;

                if (!newEvents.isEmpty()) {
                    logger.info("[poller] {} new event(s) found for workflow {} ({}:{})",
                            newEvents.size(), workflowId, appKey, triggerKey);

                    // Create a workflow run for each new event
                    for (Map<String, Object> eventData : newEvents) {
                        // ── Deduplication by message ID ────────────────────────────────
                        // Prevent the same message from triggering multiple runs even if the
                        // cursor is reset (e.g. backend restart). We use Redis SETNX (setIfAbsent)
                        // which is a single atomic command — only the first caller wins.
                        // This is safe for multi-instance deployments.
                        String messageId = extractMessageId(eventData);
                        String seenKey = null;
                        if (messageId != null) {
                            seenKey = "polling:seen:" + triggerStep.getId() + ":" + messageId;
                            // setIfAbsent = Redis SETNX: returns true only if key did NOT exist before.
                            // Atomic: no race condition possible even with multiple backend instances.
                            Boolean claimed = redisTemplate.opsForValue()
                                    .setIfAbsent(seenKey, "1", java.time.Duration.ofDays(7));
                            if (!Boolean.TRUE.equals(claimed)) {
                                logger.info("[poller] Skipping already-processed message '{}' for step {}", messageId, triggerStep.getId());
                                continue;
                            }
                        }

                        try {
                            createPollerRun(workflowId, userId, eventData, appKey, triggerKey);
                        } catch (Exception e) {
                            // If run creation fails (e.g. DB down), release the SETNX key so the
                            // message is eligible for retry on the next poll cycle.
                            // Without this, the email would be permanently lost — marked "seen"
                            // but never actually executed.
                            logger.error("[poller] Failed to create run for message '{}', releasing dedup key for retry: {}",
                                    messageId, e.getMessage());
                            if (seenKey != null) {
                                redisTemplate.delete(seenKey);
                            }
                            throw e; // Re-throw so the outer catch logs it and skips cursor advance
                        }

                        triggered++;
                        Instant eventTime = extractTimestamp(eventData);
                        if (eventTime != null && eventTime.isAfter(nextPollTime)) {
                            nextPollTime = eventTime;
                        }
                    }
                } else {
                    // If no events, advance to now() minus 1 minute buffer for provider replication lag
                    Instant safeNow = Instant.now().minus(Duration.ofMinutes(1));
                    if (safeNow.isAfter(nextPollTime)) {
                        nextPollTime = safeNow;
                    }
                }

                setLastPollTime(triggerStep.getId(), nextPollTime);
            } catch (Exception e) {
                logger.error("[poller] Failed to poll {}:{} for step {}: {}",
                        appKey, triggerKey, triggerStep.getId(), e.getMessage());
            }
        }

        return triggered;
    }

    private void createPollerRun(UUID workflowId, UUID userId,
                                  Map<String, Object> triggerData,
                                  String appKey, String triggerKey) {
        Map<String, Object> fullTriggerData = new HashMap<>(triggerData);
        fullTriggerData.put("source", "poller");
        fullTriggerData.put("triggeredAt", Instant.now().toString());
        fullTriggerData.put("triggerApp", appKey);
        fullTriggerData.put("triggerKey", triggerKey);

        UUID runId = UUID.randomUUID();
        WorkflowRun run = new WorkflowRun(runId, workflowId, userId,
                fullTriggerData, WorkflowRunStatus.PENDING);
        runRepo.save(run);

        eventPublisher.publish(
                new WorkflowRunStartedEvent(runId, workflowId, userId, fullTriggerData));

        // Enqueue for async execution via outbox
        outboxRepo.save(new OutboxEvent(
                UUID.randomUUID(),
                RedisStreamConfig.STREAM_EXECUTION_QUEUE,
                Map.of(
                        "workflowRunId", runId.toString(),
                        "workflowId", workflowId.toString(),
                        "userId", userId.toString()
                )
        ));

        logger.info("[poller] Enqueued workflow run {} for workflow {} (triggered by {}:{})",
                runId, workflowId, appKey, triggerKey);
    }

    private Instant getLastPollTime(UUID stepId) {
        String value = redisTemplate.opsForValue().get(LAST_POLL_KEY_PREFIX + stepId);
        if (value != null) {
            try {
                return Instant.parse(value);
            } catch (Exception e) {
                logger.warn("[poller] Invalid last poll time for step {}: {}", stepId, value);
            }
        }
        // First poll — look back DEFAULT_LOOKBACK minutes
        return Instant.now().minus(DEFAULT_LOOKBACK);
    }

    private void setLastPollTime(UUID stepId, Instant time) {
        redisTemplate.opsForValue().set(
                LAST_POLL_KEY_PREFIX + stepId,
                time.toString(),
                Duration.ofDays(7) // Expire after 7 days of inactivity
        );
    }

    private Instant extractTimestamp(Map<String, Object> eventData) {
        // Provider-specific timestamp field names:
        // - receivedDateTime: Microsoft Outlook (Graph API)
        // - updatedAt/createdAt: generic
        // - timestamp/time/date: legacy
        String[] keys = {"receivedDateTime", "updatedAt", "createdAt", "timestamp", "time", "date"};
        for (String key : keys) {
            Object val = eventData.get(key);
            if (val != null) {
                try {
                    return Instant.parse(val.toString());
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    /**
     * Extracts a stable, unique message/event ID from trigger event data for deduplication.
     * All pollers (Outlook, Gmail, GitHub, etc.) put the provider's native ID in the "id" field.
     */
    private String extractMessageId(Map<String, Object> eventData) {
        Object id = eventData.get("id");
        return id != null ? id.toString() : null;
    }
}
