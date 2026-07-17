package com.crescendo.webhook;

import com.crescendo.shared.infrastructure.event.WorkflowEventListener;
import com.crescendo.steps.steps_command.Steps_command;
import com.crescendo.steps.steps_command.Steps_commandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Scheduled self-healing job that finds active-workflow TRIGGER steps with no
 * corresponding active {@link Webhook} row and registers the missing webhook.
 *
 * <p><strong>Why this exists:</strong> {@link WorkflowEventListener#onWorkflowActivated}
 * registers webhooks in a secondary {@code REQUIRES_NEW} transaction that fires
 * after the parent TX commits. If that secondary TX fails (e.g. transient DB error,
 * pod restart mid-flight), the workflow is left active with no webhook registered —
 * a silent inconsistency the owner can't detect from the UI.
 *
 * <p><strong>Pattern:</strong> Mirrors {@code WorkflowRunReaper} — a periodic
 * compensating job that self-heals a single missed side-effect without saga
 * orchestration or a distributed transaction coordinator.
 *
 * <p><strong>Idempotency:</strong> Uses the same {@link Webhook#create} factory
 * and the same idempotent find-or-create check as {@code onWorkflowActivated},
 * so a race between this job and a late-firing event handler produces no duplicate
 * webhook rows (both check {@code webhookRepo.findByStepId} before inserting).
 *
 * <p><strong>Polling triggers:</strong> Steps whose {@code appKey} belongs to the
 * polling set (gmail, schedule, etc.) do not use webhook ingestion — they are skipped
 * using the same constant as {@code WorkflowEventListener}.
 */
@Component
public class WebhookReconciliationJob {

    private static final Logger logger = LoggerFactory.getLogger(WebhookReconciliationJob.class);

    /**
     * App keys whose triggers do not use webhook ingestion — must stay in sync
     * with the same constant in {@link WorkflowEventListener}.
     */
    private static final java.util.Set<String> POLLING_APP_KEYS = java.util.Set.of(
            "gmail", "microsoft-outlook", "schedule", "error-handling", "native-form",
            "github", "gitlab", "mqtt", "kafka", "rabbitmq"
    );

    private final Steps_commandRepository stepsRepo;
    private final WebhookRepository webhookRepo;

    public WebhookReconciliationJob(Steps_commandRepository stepsRepo,
                                    WebhookRepository webhookRepo) {
        this.stepsRepo = stepsRepo;
        this.webhookRepo = webhookRepo;
    }

    /**
     * Every 5 minutes: find active TRIGGER steps with no active Webhook and register them.
     *
     * <p>Runs at the same cadence as {@code WorkflowRunReaper}. The query is indexed
     * on {@code isActive}, {@code deletedAt}, and {@code stepId} — expected to be fast
     * even at scale since most steps will have a webhook.
     */
    @Scheduled(fixedRate = 300_000) // Every 5 minutes
    @Transactional
    public void reconcileMissingWebhooks() {
        List<Steps_command> orphanedTriggers = stepsRepo.findActiveTriggersWithNoWebhook();

        if (orphanedTriggers.isEmpty()) {
            return;
        }

        logger.warn("[webhook-reconcile] Found {} trigger step(s) with no active webhook — self-healing",
                orphanedTriggers.size());

        for (Steps_command step : orphanedTriggers) {
            if (POLLING_APP_KEYS.contains(step.getAppKey())) {
                // Polling triggers don't use webhook ingestion — no action needed.
                continue;
            }

            // Idempotent find-or-create — same logic as onWorkflowActivated.
            webhookRepo.findByStepId(step.getId()).ifPresentOrElse(
                    existing -> {
                        if (!existing.isActive()) {
                            existing.setActive(true);
                            webhookRepo.save(existing);
                            logger.warn("[webhook-reconcile] Reactivated webhook: webhookKey={}, stepId={}",
                                    existing.getWebhookKey(), step.getId());
                        }
                        // Already active — should not normally reach here since the query filters active=true,
                        // but safe to no-op if it does.
                    },
                    () -> {
                        // No webhook at all — create one using the same factory as onWorkflowActivated.
                        Webhook webhook = Webhook.create(UUID.randomUUID(), step.getId());
                        webhookRepo.save(webhook);
                        logger.warn("[webhook-reconcile] Registered missing webhook: webhookKey={}, stepId={}, workflowId={}",
                                webhook.getWebhookKey(), step.getId(), step.getWorkflow().getId());
                    }
            );
        }
    }
}
