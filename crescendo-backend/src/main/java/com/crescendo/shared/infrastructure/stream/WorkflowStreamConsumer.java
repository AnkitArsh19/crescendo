package com.crescendo.shared.infrastructure.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.crescendo.emailservice.metrics.DomainMetricsRollupService;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Redis Stream consumer for workflow and step domain events.
 *
 * Consumes from {@code crescendo:events:workflow} and handles durable,
 * async processing of workflow lifecycle events. This complements the
 * synchronous Spring {@code @TransactionalEventListener} handlers which
 * handle cache eviction and immediate query-side projection.
 *
 * Use cases for the stream consumer:
 *   - Cross-service event propagation (if services are split later)
 *   - Durable replay after crashes (consumer group tracks read offset)
 *   - Analytics and audit log sinks
 */
@Component
public class WorkflowStreamConsumer implements StreamListener<String, MapRecord<String, Object, Object>> {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowStreamConsumer.class);

    private final DomainMetricsRollupService rollupService;

    public WorkflowStreamConsumer(DomainMetricsRollupService rollupService) {
        this.rollupService = rollupService;
    }

    @Override
    public void onMessage(MapRecord<String, Object, Object> message) {
        Map<Object, Object> raw = message.getValue();
        String eventType = unquote(String.valueOf(raw.getOrDefault("eventType", "unknown")));
        String aggregateId = unquote(String.valueOf(raw.getOrDefault("aggregateId", "unknown")));

        logger.debug("Workflow stream event: type={}, aggregateId={}, streamId={}",
                eventType, aggregateId, message.getId());

        switch (eventType) {
            case "WorkflowCreatedEvent" ->
                    logger.info("[stream] Workflow created: {}", aggregateId);
            case "WorkflowUpdatedEvent" ->
                    logger.info("[stream] Workflow updated: {}", aggregateId);
            case "WorkflowDeletedEvent" ->
                    logger.info("[stream] Workflow deleted: {}", aggregateId);
            case "WorkflowActivatedEvent" ->
                    logger.info("[stream] Workflow activated: {}", aggregateId);
            case "WorkflowDeactivatedEvent" ->
                    logger.info("[stream] Workflow deactivated: {}", aggregateId);
            case "WorkflowRunStartedEvent" ->
                    logger.info("[stream] Workflow run started: runId={}, workflowId={}",
                            aggregateId, unquote(String.valueOf(raw.getOrDefault("workflowId", "unknown"))));
            case "WorkflowRunCompletedEvent" ->
                    logger.info("[stream] Workflow run completed: runId={}, status={}",
                            aggregateId, unquote(String.valueOf(raw.getOrDefault("status", "unknown"))));
            case "StepCreatedEvent", "StepUpdatedEvent", "StepDeletedEvent", "StepReorderedEvent" ->
                    logger.info("[stream] Step event {}: stepId={}, workflowId={}",
                            eventType, aggregateId, unquote(String.valueOf(raw.getOrDefault("workflowId", "unknown"))));
            case "StepRunCompletedEvent" ->
                    logger.info("[stream] Step run completed: stepRunId={}, status={}",
                            aggregateId, unquote(String.valueOf(raw.getOrDefault("status", "unknown"))));
            case "EmailDeliveredEvent", "EmailBouncedEvent", "EmailComplainedEvent" -> {
                String domainIdStr = unquote(String.valueOf(raw.getOrDefault("domainId", "")));
                if (domainIdStr != null && !domainIdStr.isEmpty() && !domainIdStr.equals("null")) {
                    rollupService.recordEvent(UUID.fromString(domainIdStr), eventType);
                }
            }
            default ->
                    logger.warn("[stream] Unknown workflow event type: {}", eventType);
        }
    }

    private static String unquote(String s) {
        if (s != null && s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
