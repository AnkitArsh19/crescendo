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
import com.crescendo.workflow.workflow_command.Workflow_command;
import com.crescendo.workflow.workflow_command.Workflow_commandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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
     * App keys whose triggers do not use generic webhook ingestion.
     */
    private static final Set<String> POLLING_APP_KEYS = Set.of(
            "gmail", "microsoft-outlook", "schedule", "error-handling", "native-form",
            "github", "gitlab", "mqtt", "kafka", "rabbitmq"
    );

    private final Steps_commandRepository stepsRepo;
    private final WebhookRepository webhookRepo;
    private final Workflow_commandRepository workflowRepo;
    private final CacheManager cacheManager;

    public WorkflowEventListener(Steps_commandRepository stepsRepo,
                                 WebhookRepository webhookRepo,
                                 Workflow_commandRepository workflowRepo,
                                 CacheManager cacheManager) {
        this.stepsRepo = stepsRepo;
        this.webhookRepo = webhookRepo;
        this.workflowRepo = workflowRepo;
        this.cacheManager = cacheManager;
    }

    @TransactionalEventListener
    public void onWorkflowCreated(WorkflowCreatedEvent event) {
        logger.info("Workflow created: workflowId={}, name={}, userId={}, guest={}",
                event.aggregateId(), event.getWorkflowName(), event.getUserId(), event.isGuestWorkflow());
        // Downstream: analytics tracking for workflow creation metrics
    }

    @TransactionalEventListener
    public void onWorkflowUpdated(WorkflowUpdatedEvent event) {
        logger.info("Workflow updated: workflowId={}", event.aggregateId());
        evictWorkflowCaches(event.aggregateId());
    }

    @TransactionalEventListener
    public void onWorkflowDeleted(WorkflowDeletedEvent event) {
        logger.info("Workflow deleted: workflowId={}", event.aggregateId());
        evictWorkflowCaches(event.aggregateId());
        // Downstream: cancel any scheduled triggers for this workflow
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    public void onWorkflowActivated(WorkflowActivatedEvent event) {
        UUID workflowId = event.aggregateId();
        logger.info("Workflow activated: workflowId={}", workflowId);
        evictWorkflowCaches(workflowId);

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
    public void onWorkflowDeactivated(WorkflowDeactivatedEvent event) {
        UUID workflowId = event.aggregateId();
        logger.info("Workflow deactivated: workflowId={}", workflowId);
        evictWorkflowCaches(workflowId);

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
    public void onStepCreated(StepCreatedEvent event) {
        logger.info("Step created: stepId={}, workflowId={}, name={}",
                event.aggregateId(), event.getWorkflowId(), event.getStepName());
        evictWorkflowCaches(event.getWorkflowId());
    }

    @TransactionalEventListener
    public void onStepUpdated(StepUpdatedEvent event) {
        logger.info("Step updated: stepId={}, workflowId={}", event.aggregateId(), event.getWorkflowId());
        evictWorkflowCaches(event.getWorkflowId());
    }

    @TransactionalEventListener
    public void onStepDeleted(StepDeletedEvent event) {
        logger.info("Step deleted: stepId={}, workflowId={}", event.aggregateId(), event.getWorkflowId());
        evictWorkflowCaches(event.getWorkflowId());
    }

    @TransactionalEventListener
    public void onStepReordered(StepReorderedEvent event) {
        logger.info("Step reordered: stepId={}, workflowId={}", event.aggregateId(), event.getWorkflowId());
        evictWorkflowCaches(event.getWorkflowId());
    }

    private void evictWorkflowCaches(UUID workflowId) {
        Cache cache = cacheManager.getCache("workflows");
        if (cache == null) {
            return;
        }

        Workflow_command workflow = workflowRepo.findById(workflowId).orElse(null);
        if (workflow == null) {
            logger.warn("[cache] Workflow {} not found for eviction", workflowId);
            return;
        }

        if (workflow.getUser() != null) {
            cache.evict("detail:v2:" + workflow.getUser().getId() + ":" + workflowId);
        }
        if (workflow.getGuestSessionId() != null) {
            cache.evict("guest-detail:v2:" + workflow.getGuestSessionId() + ":" + workflowId);
        }
    }
}
