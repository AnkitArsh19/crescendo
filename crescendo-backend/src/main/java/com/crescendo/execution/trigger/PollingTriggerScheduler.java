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
import com.crescendo.shared.infrastructure.event.RedisDomainEventPublisher;
import com.crescendo.steps.steps_command.Steps_command;
import com.crescendo.steps.steps_command.Steps_commandRepository;
import com.crescendo.workflow.workflow_command.Workflow_command;
import com.crescendo.workflow.workflow_command.Workflow_commandRepository;
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
    private static final Duration DEFAULT_LOOKBACK = Duration.ofMinutes(5);

    private final List<TriggerPoller> pollers;
    private final Workflow_commandRepository workflowRepo;
    private final Steps_commandRepository stepsRepo;
    private final Connections_commandRepository connectionsRepo;
    private final OAuthTokenRefreshService tokenService;
    private final WorkflowRunRepository runRepo;
    private final DomainEventPublisher eventPublisher;
    private final RedisDomainEventPublisher redisEventPublisher;
    private final StringRedisTemplate redisTemplate;

    public PollingTriggerScheduler(List<TriggerPoller> pollers,
                                    Workflow_commandRepository workflowRepo,
                                    Steps_commandRepository stepsRepo,
                                    Connections_commandRepository connectionsRepo,
                                    OAuthTokenRefreshService tokenService,
                                    WorkflowRunRepository runRepo,
                                    DomainEventPublisher eventPublisher,
                                    RedisDomainEventPublisher redisEventPublisher,
                                    StringRedisTemplate redisTemplate) {
        this.pollers = pollers;
        this.workflowRepo = workflowRepo;
        this.stepsRepo = stepsRepo;
        this.connectionsRepo = connectionsRepo;
        this.tokenService = tokenService;
        this.runRepo = runRepo;
        this.eventPublisher = eventPublisher;
        this.redisEventPublisher = redisEventPublisher;
        this.redisTemplate = redisTemplate;
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

            UUID connectionId = triggerStep.getConnectionId();
            if (connectionId == null) {
                logger.warn("[poller] Trigger step {} has no connectionId, skipping", triggerStep.getId());
                continue;
            }

            try {
                // Get valid OAuth credentials
                Connections_command connection = connectionsRepo.findByIdAndUser_Id(connectionId, userId)
                        .orElse(null);
                if (connection == null) {
                    logger.warn("[poller] Connection {} not found for user {}", connectionId, userId);
                    continue;
                }

                Map<String, Object> credentials = tokenService.getValidCredentials(connection);

                // Get the last poll time from Redis
                Instant lastPollTime = getLastPollTime(triggerStep.getId());

                // Poll for new events
                Map<String, Object> stepConfig = triggerStep.getConfiguration() != null
                        ? triggerStep.getConfiguration()
                        : Map.of();

                logger.info("[poller] Polling {}:{} — stepId={}, connectionId={}, config={}, lastPoll={}",
                        appKey, triggerKey, triggerStep.getId(), connectionId, stepConfig, lastPollTime);

                List<Map<String, Object>> newEvents = poller.poll(credentials, stepConfig, lastPollTime);

                // Update last poll time regardless of whether events were found
                setLastPollTime(triggerStep.getId(), Instant.now());

                if (!newEvents.isEmpty()) {
                    logger.info("[poller] {} new event(s) found for workflow {} ({}:{})",
                            newEvents.size(), workflowId, appKey, triggerKey);

                    // Create a workflow run for each new event
                    for (Map<String, Object> eventData : newEvents) {
                        createPollerRun(workflowId, userId, eventData, appKey, triggerKey);
                        triggered++;
                    }
                }
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

        // Enqueue for async execution
        redisEventPublisher.enqueueExecution(Map.of(
                "workflowRunId", runId.toString(),
                "workflowId", workflowId.toString(),
                "userId", userId.toString()
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
}
