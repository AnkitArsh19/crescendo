package com.crescendo.execution.test;

import com.crescendo.execution.resource.ResourceFetchService;
import com.crescendo.execution.resource.ResourceOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Provides safe sample records for workflow triggers (similar to Zapier's "Test Trigger").
 * Never executes destructive webhook registrations or mutating external actions.
 */
@Service
public class TriggerSampleService {

    private static final Logger logger = LoggerFactory.getLogger(TriggerSampleService.class);

    private final ResourceFetchService resourceFetchService;

    public TriggerSampleService(ResourceFetchService resourceFetchService) {
        this.resourceFetchService = resourceFetchService;
    }

    public Map<String, Object> getTriggerSample(String appKey, String triggerKey, String connectionId,
                                                Map<String, Object> configuration, UUID userId) {
        Map<String, Object> config = configuration == null ? Map.of() : configuration;

        // 1. Built-in special triggers
        if ("schedule".equalsIgnoreCase(appKey)) {
            return Map.of(
                    "scheduledTime", Instant.now().toString(),
                    "timestamp", System.currentTimeMillis(),
                    "timezone", config.getOrDefault("timezone", "UTC"),
                    "interval", config.getOrDefault("interval", "1h")
            );
        }

        if ("webhook".equalsIgnoreCase(appKey) || "catch-hook".equalsIgnoreCase(triggerKey)) {
            return Map.of(
                    "id", "evt_" + UUID.randomUUID().toString().substring(0, 8),
                    "timestamp", Instant.now().toString(),
                    "headers", Map.of("content-type", "application/json", "user-agent", "WebhookClient/1.0"),
                    "body", Map.of(
                            "event", "item.created",
                            "id", "rec_987654",
                            "name", "Sample Payload",
                            "email", "user@example.com",
                            "status", "active",
                            "amount", 129.99
                    ),
                    "query", Map.of("source", "test")
            );
        }

        if ("rss".equalsIgnoreCase(appKey)) {
            return Map.of(
                    "id", "item-" + UUID.randomUUID().toString().substring(0, 6),
                    "title", "Latest News & Updates Announcement",
                    "link", "https://example.com/news/sample-post",
                    "pubDate", Instant.now().toString(),
                    "author", "Editorial Team",
                    "summary", "This is a sample feed item retrieved for workflow step testing."
            );
        }

        if ("nativeform".equalsIgnoreCase(appKey) || "typeform".equalsIgnoreCase(appKey)) {
            return Map.of(
                    "submissionId", "sub_" + UUID.randomUUID().toString().substring(0, 8),
                    "submittedAt", Instant.now().toString(),
                    "answers", Map.of(
                            "fullName", "Alex Mercer",
                            "email", "alex.mercer@example.com",
                            "company", "Acme Corp",
                            "feedback", "Everything looks great!"
                    )
            );
        }

        // 2. Try fetching a live sample from an existing ResourceProvider if available and connected
        if (connectionId != null && !connectionId.isBlank() && userId != null) {
            try {
                UUID connUuid = UUID.fromString(connectionId);
                // Check if resource provider has listable resources that match the trigger concept
                List<ResourceOption> options = resourceFetchService.fetchResources(
                        appKey, sampleResourceTypeForTrigger(triggerKey), connUuid, userId, Map.of());
                if (options != null && !options.isEmpty()) {
                    ResourceOption sample = options.getFirst();
                    return Map.of(
                            "id", sample.id(),
                            "name", sample.label(),
                            "description", sample.description() != null ? sample.description() : "",
                            "retrievedAt", Instant.now().toString(),
                            "isRealSample", true
                    );
                }
            } catch (Exception e) {
                logger.debug("[trigger-sample] Could not fetch live sample for {}/{}: {}", appKey, triggerKey, e.getMessage());
            }
        }

        // 3. Fallback: intelligent generic trigger record
        return generateGenericTriggerRecord(appKey, triggerKey, config);
    }

    private String sampleResourceTypeForTrigger(String triggerKey) {
        String key = triggerKey.toLowerCase(Locale.ROOT);
        if (key.contains("channel")) return "channels";
        if (key.contains("database") || key.contains("table")) return "databases";
        if (key.contains("sheet") || key.contains("row")) return "sheets";
        if (key.contains("playlist") || key.contains("track")) return "playlists";
        if (key.contains("ticket")) return "tickets";
        if (key.contains("contact") || key.contains("user")) return "contacts";
        if (key.contains("deal")) return "deals";
        if (key.contains("project") || key.contains("task")) return "tasks";
        if (key.contains("activity")) return "activities";
        return "items";
    }

    private Map<String, Object> generateGenericTriggerRecord(String appKey, String triggerKey, Map<String, Object> config) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", "rec_" + UUID.randomUUID().toString().substring(0, 8));
        record.put("event", triggerKey);
        record.put("app", appKey);
        record.put("createdAt", Instant.now().toString());

        // Contextual sample fields based on common trigger names
        String t = triggerKey.toLowerCase(Locale.ROOT);
        if (t.contains("email") || t.contains("message")) {
            record.put("from", "sender@example.com");
            record.put("to", "recipient@example.com");
            record.put("subject", "Sample Message Subject");
            record.put("body", "Sample message body text for workflow mapping.");
        } else if (t.contains("user") || t.contains("contact") || t.contains("follower")) {
            record.put("name", "Sample User");
            record.put("email", "sample.user@example.com");
            record.put("handle", "@sample_user");
        } else if (t.contains("order") || t.contains("payment")) {
            record.put("orderId", "ord_78901");
            record.put("amount", 99.00);
            record.put("currency", "USD");
            record.put("customerEmail", "customer@example.com");
        } else if (t.contains("task") || t.contains("issue") || t.contains("ticket")) {
            record.put("title", "Sample Ticket / Task Title");
            record.put("priority", "high");
            record.put("status", "open");
        } else {
            record.put("title", "Sample " + appKey + " " + triggerKey);
            record.put("status", "active");
        }

        if (!config.isEmpty()) {
            record.put("configuredTarget", config);
        }

        return record;
    }
}
