package com.crescendo.apps.errorhandling;

import com.crescendo.config.RedisStreamConfig;
import com.crescendo.enums.StepType;
import com.crescendo.enums.WorkflowRunStatus;
import com.crescendo.logbook.domain_event.WorkflowRunCompletedEvent;
import com.crescendo.logbook.domain_event.WorkflowRunStartedEvent;
import com.crescendo.logbook.outbox.OutboxEvent;
import com.crescendo.logbook.outbox.OutboxEventRepository;
import com.crescendo.logbook.workflow_run.WorkflowRun;
import com.crescendo.logbook.workflow_run.WorkflowRunRepository;
import com.crescendo.shared.domain.event.DomainEventPublisher;
import com.crescendo.steps.steps_command.Steps_command;
import com.crescendo.steps.steps_command.Steps_commandRepository;
import com.crescendo.workflow.workflow_command.Workflow_command;
import com.crescendo.workflow.workflow_command.Workflow_commandRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ErrorTriggerListener {

    private static final Logger logger = LoggerFactory.getLogger(ErrorTriggerListener.class);

    private final Workflow_commandRepository workflowRepo;
    private final Steps_commandRepository stepsRepo;
    private final WorkflowRunRepository runRepo;
    private final OutboxEventRepository outboxRepo;
    private final DomainEventPublisher eventPublisher;

    public ErrorTriggerListener(Workflow_commandRepository workflowRepo,
                                Steps_commandRepository stepsRepo,
                                WorkflowRunRepository runRepo,
                                OutboxEventRepository outboxRepo,
                                DomainEventPublisher eventPublisher) {
        this.workflowRepo = workflowRepo;
        this.stepsRepo = stepsRepo;
        this.runRepo = runRepo;
        this.outboxRepo = outboxRepo;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    @Transactional
    public void onWorkflowRunFailed(WorkflowRunCompletedEvent event) {
        if (event.getStatus() != WorkflowRunStatus.FAILED) {
            return;
        }

        List<Workflow_command> candidates = workflowRepo.findAll().stream()
                .filter(workflow -> workflow.isActive() && workflow.getDeletedAt() == null && workflow.getUser() != null)
                .filter(workflow -> workflow.getUser().getId().equals(event.getUserId()))
                .filter(workflow -> !workflow.getId().equals(event.getWorkflowId()))
                .toList();

        for (Workflow_command workflow : candidates) {
            Steps_command trigger = findErrorTrigger(workflow.getId());
            if (trigger == null || !matchesWatchedWorkflow(trigger, event.getWorkflowId())) {
                continue;
            }
            enqueueErrorWorkflow(workflow, event);
        }
    }

    private Steps_command findErrorTrigger(UUID workflowId) {
        return stepsRepo.findActiveByWorkflowId(workflowId).stream()
                .filter(step -> step.getType() == StepType.TRIGGER)
                .filter(step -> "errorhandling".equals(step.getAppKey()))
                .filter(step -> "error-trigger".equals(step.getActionKey()))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesWatchedWorkflow(Steps_command trigger, UUID failedWorkflowId) {
        Map<String, Object> config = trigger.getConfiguration();
        if (config == null || config.get("workflowId") == null || String.valueOf(config.get("workflowId")).isBlank()) {
            return true;
        }
        try {
            return UUID.fromString(String.valueOf(config.get("workflowId"))).equals(failedWorkflowId);
        } catch (IllegalArgumentException e) {
            logger.warn("[error-trigger] Invalid workflowId filter on step {}: {}", trigger.getId(), config.get("workflowId"));
            return false;
        }
    }

    private void enqueueErrorWorkflow(Workflow_command workflow, WorkflowRunCompletedEvent failedRun) {
        UUID runId = UUID.randomUUID();
        UUID workflowId = workflow.getId();
        UUID userId = workflow.getUser().getId();

        Map<String, Object> triggerData = new HashMap<>();
        triggerData.put("source", "error-trigger");
        triggerData.put("triggeredAt", Instant.now().toString());
        triggerData.put("failedWorkflowRunId", failedRun.aggregateId().toString());
        triggerData.put("failedWorkflowId", failedRun.getWorkflowId().toString());
        triggerData.put("errorMessage", failedRun.getErrorMessage());

        WorkflowRun run = new WorkflowRun(runId, workflowId, userId, triggerData, WorkflowRunStatus.PENDING);
        runRepo.save(run);

        eventPublisher.publish(new WorkflowRunStartedEvent(runId, workflowId, userId, triggerData));

        outboxRepo.save(new OutboxEvent(
                UUID.randomUUID(),
                RedisStreamConfig.STREAM_EXECUTION_QUEUE,
                Map.of(
                        "workflowRunId", runId.toString(),
                        "workflowId", workflowId.toString(),
                        "userId", userId.toString()
                )
        ));

        logger.info("[error-trigger] Enqueued error workflow {} for failed workflow run {}",
                workflowId, failedRun.aggregateId());
    }
}
