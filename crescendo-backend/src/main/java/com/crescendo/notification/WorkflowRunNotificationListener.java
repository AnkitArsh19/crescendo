package com.crescendo.notification;

import com.crescendo.enums.WorkflowRunStatus;
import com.crescendo.logbook.domain_event.WorkflowRunCompletedEvent;
import com.crescendo.workflow.workflow_query.Workflow_query;
import com.crescendo.workflow.workflow_query.Workflow_queryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listens to WorkflowRunCompletedEvent and creates in-app & push notifications
 * according to user and workflow preferences.
 */
@Component
public class WorkflowRunNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(WorkflowRunNotificationListener.class);

    private final UserNotificationService userNotificationService;
    private final Workflow_queryRepository workflowQueryRepository;

    public WorkflowRunNotificationListener(
            UserNotificationService userNotificationService,
            Workflow_queryRepository workflowQueryRepository) {
        this.userNotificationService = userNotificationService;
        this.workflowQueryRepository = workflowQueryRepository;
    }

    @EventListener
    public void onWorkflowRunCompleted(WorkflowRunCompletedEvent event) {
        UUID userId = event.getUserId();
        if (userId == null) {
            return; // Skip guest sessions
        }

        NotificationType type = mapStatusToType(event.getStatus());
        if (type == null) {
            return;
        }

        try {
            String workflowName = "Workflow";
            if (event.getWorkflowId() != null) {
                workflowName = workflowQueryRepository.findById(event.getWorkflowId())
                        .map(Workflow_query::getName)
                        .orElse("Workflow");
            }

            String title;
            String body;
            if (type == NotificationType.WORKFLOW_RUN_SUCCESS) {
                title = "Workflow Run Succeeded";
                body = "Workflow \"" + workflowName + "\" completed successfully.";
            } else if (type == NotificationType.WORKFLOW_RUN_FAILED) {
                title = "Workflow Run Failed";
                String err = (event.getErrorMessage() != null && !event.getErrorMessage().isBlank())
                        ? ": " + event.getErrorMessage()
                        : ".";
                body = "Workflow \"" + workflowName + "\" failed" + err;
            } else {
                title = "Workflow Run Cancelled";
                body = "Workflow \"" + workflowName + "\" was cancelled.";
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("workflowId", event.getWorkflowId() != null ? event.getWorkflowId().toString() : "");
            metadata.put("runId", event.aggregateId() != null ? event.aggregateId().toString() : "");
            metadata.put("workflowName", workflowName);
            metadata.put("status", event.getStatus().name());
            if (event.getErrorMessage() != null) {
                metadata.put("errorMessage", event.getErrorMessage());
            }

            userNotificationService.create(userId, type, title, body, metadata);
        } catch (Exception e) {
            log.warn("Failed to process workflow run notification for runId {}: {}", event.aggregateId(), e.getMessage());
        }
    }

    private NotificationType mapStatusToType(WorkflowRunStatus status) {
        if (status == null) return null;
        return switch (status) {
            case SUCCESS -> NotificationType.WORKFLOW_RUN_SUCCESS;
            case FAILED -> NotificationType.WORKFLOW_RUN_FAILED;
            case CANCELLED -> NotificationType.WORKFLOW_RUN_CANCELLED;
            default -> null;
        };
    }
}
