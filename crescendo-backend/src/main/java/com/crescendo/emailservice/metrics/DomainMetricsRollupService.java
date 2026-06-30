package com.crescendo.emailservice.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Batches domain metrics events to prevent row contention.
 * Flushes to the database asynchronously on a schedule.
 */
@Service
public class DomainMetricsRollupService {

    private static final Logger logger = LoggerFactory.getLogger(DomainMetricsRollupService.class);

    private final JdbcTemplate jdbcTemplate;

    // Buffer: key = domainId + "|" + date
    private final ConcurrentHashMap<String, DomainMetricsQuery> buffer = new ConcurrentHashMap<>();

    public DomainMetricsRollupService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void recordEvent(UUID domainId, String eventType) {
        if (domainId == null) return;
        LocalDate today = LocalDate.now();
        String key = domainId.toString() + "|" + today.toString();

        DomainMetricsQuery metrics = buffer.computeIfAbsent(key, k -> new DomainMetricsQuery(domainId, today));

        switch (eventType) {
            case "EmailDeliveredEvent" -> metrics.incrementSent();
            case "EmailBouncedEvent" -> metrics.incrementBounce();
            case "EmailComplainedEvent" -> metrics.incrementSpam();
            default -> {}
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void flush() {
        if (buffer.isEmpty()) return;

        // Take a snapshot of keys and remove them from buffer
        for (String key : buffer.keySet()) {
            DomainMetricsQuery metrics = buffer.remove(key);
            if (metrics == null) continue;

            upsertMetrics(metrics);
        }
    }

    private void upsertMetrics(DomainMetricsQuery metrics) {
        try {
            String sql = """
                INSERT INTO daily_domain_metrics_query (domain_id, date, sent_count, bounce_count, spam_count)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (domain_id, date) DO UPDATE SET
                    sent_count = daily_domain_metrics_query.sent_count + EXCLUDED.sent_count,
                    bounce_count = daily_domain_metrics_query.bounce_count + EXCLUDED.bounce_count,
                    spam_count = daily_domain_metrics_query.spam_count + EXCLUDED.spam_count
            """;
            
            jdbcTemplate.update(sql, 
                metrics.getDomainId(), 
                metrics.getDate(), 
                metrics.getSentCount(), 
                metrics.getBounceCount(), 
                metrics.getSpamCount()
            );
            
            logger.debug("Flushed metrics for domain {}: sent +{}, bounce +{}, spam +{}", 
                metrics.getDomainId(), metrics.getSentCount(), metrics.getBounceCount(), metrics.getSpamCount());
        } catch (Exception e) {
            logger.error("Failed to upsert domain metrics for domain {}: {}", metrics.getDomainId(), e.getMessage());
            // In a real system, we might re-enqueue or handle failure differently. 
            // For now, logging is sufficient.
        }
    }
}
