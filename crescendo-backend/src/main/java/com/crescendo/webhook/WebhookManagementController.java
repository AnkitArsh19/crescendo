package com.crescendo.webhook;

import com.crescendo.steps.steps_command.Steps_command;
import com.crescendo.steps.steps_command.Steps_commandRepository;
import com.crescendo.workflow.workflow_command.Workflow_command;
import com.crescendo.workflow.workflow_command.Workflow_commandRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static com.crescendo.security.AuthenticatedUser.userId;

import java.util.List;
import java.util.UUID;

/**
 * Authenticated webhook management endpoints.
 *
 * These let users view and manage webhooks for their workflows.
 * Webhook creation is handled automatically on workflow activation — these
 * endpoints provide visibility and manual control (toggle, regenerate key).
 *
 *   GET    /workflows/{workflowId}/webhooks               — list webhooks for a workflow
 *   GET    /workflows/{workflowId}/webhooks/{webhookId}    — get webhook detail
 *   POST   /workflows/{workflowId}/webhooks/{webhookId}/toggle   — toggle active/inactive
 *   POST   /workflows/{workflowId}/webhooks/{webhookId}/regenerate — regenerate webhook key
 */
@RestController
@RequestMapping("/workflows/{workflowId}/webhooks")
public class WebhookManagementController {

    private final WebhookRepository webhookRepo;
    private final Steps_commandRepository stepsRepo;
    private final Workflow_commandRepository workflowRepo;

    @Value("${app.webhook.base-url:}")
    private String webhookBaseUrl;

    public WebhookManagementController(WebhookRepository webhookRepo,
                                       Steps_commandRepository stepsRepo,
                                       Workflow_commandRepository workflowRepo) {
        this.webhookRepo = webhookRepo;
        this.stepsRepo = stepsRepo;
        this.workflowRepo = workflowRepo;
    }

    @GetMapping
    public ResponseEntity<List<WebhookDto.WebhookResponse>> listWebhooks(
            @PathVariable UUID workflowId,
            Authentication auth) {
        verifyWorkflowOwnership(userId(auth), workflowId);

        List<UUID> stepIds = stepsRepo.findActiveByWorkflowId(workflowId)
                .stream()
                .map(Steps_command::getId)
                .toList();

        if (stepIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<WebhookDto.WebhookResponse> webhooks = webhookRepo.findByStepIdIn(stepIds)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(webhooks);
    }

    @GetMapping("/{webhookId}")
    public ResponseEntity<WebhookDto.WebhookResponse> getWebhook(
            @PathVariable UUID workflowId,
            @PathVariable UUID webhookId,
            Authentication auth) {
        verifyWorkflowOwnership(userId(auth), workflowId);

        Webhook webhook = findWebhookForWorkflow(webhookId, workflowId);
        return ResponseEntity.ok(toResponse(webhook));
    }

    @PostMapping("/{webhookId}/toggle")
    public ResponseEntity<WebhookDto.WebhookResponse> toggleWebhook(
            @PathVariable UUID workflowId,
            @PathVariable UUID webhookId,
            Authentication auth) {
        verifyWorkflowOwnership(userId(auth), workflowId);

        Webhook webhook = findWebhookForWorkflow(webhookId, workflowId);
        webhook.setActive(!webhook.isActive());
        webhookRepo.save(webhook);

        return ResponseEntity.ok(toResponse(webhook));
    }

    @PostMapping("/{webhookId}/regenerate")
    public ResponseEntity<WebhookDto.WebhookResponse> regenerateWebhookKey(
            @PathVariable UUID workflowId,
            @PathVariable UUID webhookId,
            Authentication auth) {
        verifyWorkflowOwnership(userId(auth), workflowId);

        Webhook webhook = findWebhookForWorkflow(webhookId, workflowId);
        webhook.setWebhookKey(com.crescendo.shared.domain.valueobject.WebhookKey.generate());
        webhookRepo.save(webhook);

        return ResponseEntity.ok(toResponse(webhook));
    }

    // HELPERS

    private void verifyWorkflowOwnership(UUID userId, UUID workflowId) {
        Workflow_command workflow = workflowRepo.findByIdAndDeletedAtIsNull(workflowId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));

        if (workflow.getUser() == null || !workflow.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this workflow");
        }
    }

    private Webhook findWebhookForWorkflow(UUID webhookId, UUID workflowId) {
        Webhook webhook = webhookRepo.findById(webhookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Webhook not found"));

        // Verify the webhook's step belongs to this workflow
        Steps_command step = stepsRepo.findByIdAndDeletedAtIsNull(webhook.getStepId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Webhook step not found"));

        if (!step.getWorkflow().getId().equals(workflowId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Webhook not found for this workflow");
        }

        return webhook;
    }

    private WebhookDto.WebhookResponse toResponse(Webhook w) {
        String url = (webhookBaseUrl != null && !webhookBaseUrl.isBlank())
                ? webhookBaseUrl + "/webhooks/" + w.getWebhookKey()
                : "/webhooks/" + w.getWebhookKey();

        return new WebhookDto.WebhookResponse(
                w.getId(),
                w.getWebhookKey(),
                w.getStepId(),
                w.isActive(),
                url,
                w.getCreatedAt()
        );
    }

}
