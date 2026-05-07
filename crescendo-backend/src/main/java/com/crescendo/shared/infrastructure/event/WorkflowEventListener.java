package com.crescendo.shared.infrastructure.event;

import com.crescendo.enums.StepType;
import com.crescendo.steps.domain_event.StepCreatedEvent;
import com.crescendo.steps.domain_event.StepDeletedEvent;
import com.crescendo.steps.domain_event.StepReorderedEvent;
import com.crescendo.steps.domain_event.StepUpdatedEvent;
import com.crescendo.steps.steps_command.Steps_command;
import com.crescendo.steps.steps_command.Steps_commandRepository;
import com.crescendo.webhook.Webhook;
import com.crescendo.webhook.WebhookRepository;
import com.crescendo.workflow.domain_event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Listens for workflow and step domain events.
 *
 * Responsibilities:
 *   - Evict stale cached workflow and step data
 *   - Log workflow lifecycle events
 *   - Register/unregister webhook triggers on activate/deactivate
 */
@Component
public class WorkflowEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowEventListener.class);

    /**
     * App keys that use polling instead of webhooks.
     * Triggers from these apps are handled by PollingTriggerScheduler,
     * so we skip webhook registration for them.
     */
    private static final Set<String> POLLING_APP_KEYS = Set.of(
            "gmail", "microsoft-outlook"
    );

    private final Steps_commandRepository stepsRepo;
    private final WebhookRepository webhookRepo;

    public WorkflowEventListener(Steps_commandRepository stepsRepo,
                                 WebhookRepository webhookRepo) {
        this.stepsRepo = stepsRepo;
        this.webhookRepo = webhookRepo;
    }

    @TransactionalEventListener
    public void onWorkflowCreated(WorkflowCreatedEvent event) {
        logger.info("Workflow created: workflowId={}, name={}, userId={}, guest={}",
                event.aggregateId(), event.getWorkflowName(), event.getUserId(), event.isGuestWorkflow());
        // Downstream: analytics tracking for workflow creation metrics
    }

    @TransactionalEventListener
    @Caching(evict = {
            @CacheEvict(value = "workflows", allEntries = true),
            @CacheEvict(value = "steps", allEntries = true)
    })
    public void onWorkflowUpdated(WorkflowUpdatedEvent event) {
        logger.info("Workflow updated: workflowId={}", event.aggregateId());
    }

    @TransactionalEventListener
    @Caching(evict = {
            @CacheEvict(value = "workflows", allEntries = true),
            @CacheEvict(value = "steps", allEntries = true)
    })
    public void onWorkflowDeleted(WorkflowDeletedEvent event) {
        logger.info("Workflow deleted: workflowId={}", event.aggregateId());
        // Downstream: cancel any scheduled triggers for this workflow
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    @CacheEvict(value = "workflows", allEntries = true)
    public void onWorkflowActivated(WorkflowActivatedEvent event) {
        UUID workflowId = event.aggregateId();
        logger.info("Workflow activated: workflowId={}", workflowId);

        // Find all TRIGGER steps for this workflow and register webhooks
        List<Steps_command> triggerSteps = stepsRepo.findActiveByWorkflowId(workflowId)
                .stream()
                .filter(s -> s.getType() == StepType.TRIGGER)
                .toList();

        for (Steps_command step : triggerSteps) {
            // Skip webhook registration for polling-based triggers
            if (POLLING_APP_KEYS.contains(step.getAppKey())) {
                logger.info("Skipping webhook for polling trigger: appKey={}, stepId={}",
                        step.getAppKey(), step.getId());
                continue;
            }

            Webhook webhook = webhookRepo.findByStepId(step.getId()).orElse(null);
            if (webhook == null) {
                webhook = Webhook.create(UUID.randomUUID(), step.getId());
                webhookRepo.save(webhook);
                logger.info("Webhook registered: webhookKey={}, stepId={}", webhook.getWebhookKey(), step.getId());
            } else if (!webhook.isActive()) {
                webhook.setActive(true);
                logger.info("Webhook reactivated: webhookKey={}, stepId={}", webhook.getWebhookKey(), step.getId());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    @CacheEvict(value = "workflows", allEntries = true)
    public void onWorkflowDeactivated(WorkflowDeactivatedEvent event) {
        UUID workflowId = event.aggregateId();
        logger.info("Workflow deactivated: workflowId={}", workflowId);

        // Deactivate all webhooks for this workflow's trigger steps
        List<UUID> triggerStepIds = stepsRepo.findActiveByWorkflowId(workflowId)
                .stream()
                .filter(s -> s.getType() == StepType.TRIGGER)
                .map(Steps_command::getId)
                .toList();

        if (!triggerStepIds.isEmpty()) {
            List<Webhook> webhooks = webhookRepo.findByStepIdIn(triggerStepIds);
            for (Webhook webhook : webhooks) {
                if (webhook.isActive()) {
                    webhook.setActive(false);
                    logger.info("Webhook deactivated: webhookKey={}, stepId={}", webhook.getWebhookKey(), webhook.getStepId());
                }
            }
        }
    }

    // ---- STEP EVENTS ----

    @TransactionalEventListener
    @CacheEvict(value = "workflows", allEntries = true)
    public void onStepCreated(StepCreatedEvent event) {
        logger.info("Step created: stepId={}, workflowId={}, name={}",
                event.aggregateId(), event.getWorkflowId(), event.getStepName());
    }

    @TransactionalEventListener
    @CacheEvict(value = "workflows", allEntries = true)
    public void onStepUpdated(StepUpdatedEvent event) {
        logger.info("Step updated: stepId={}, workflowId={}", event.aggregateId(), event.getWorkflowId());
    }

    @TransactionalEventListener
    @CacheEvict(value = "workflows", allEntries = true)
    public void onStepDeleted(StepDeletedEvent event) {
        logger.info("Step deleted: stepId={}, workflowId={}", event.aggregateId(), event.getWorkflowId());
    }

    @TransactionalEventListener
    @CacheEvict(value = "workflows", allEntries = true)
    public void onStepReordered(StepReorderedEvent event) {
        logger.info("Step reordered: stepId={}, workflowId={}", event.aggregateId(), event.getWorkflowId());
    }
}
