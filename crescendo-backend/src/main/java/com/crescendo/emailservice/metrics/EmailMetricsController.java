package com.crescendo.emailservice.metrics;

import com.crescendo.emailservice.email_log.EmailLogRepository;
import com.crescendo.enums.EmailStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.crescendo.security.AuthenticatedUser.userId;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/settings/email-metrics")
public class EmailMetricsController {

    private final EmailLogRepository emailLogRepo;

    public EmailMetricsController(EmailLogRepository emailLogRepo) {
        this.emailLogRepo = emailLogRepo;
    }

    @GetMapping
    public ResponseEntity<EmailMetricsDto.MetricsResponse> metrics(
            @RequestParam(defaultValue = "30") int days,
            Authentication auth) {

        UUID userId = userId(auth);

        // Summary: counts by status
        Map<String, Long> statusCounts = new HashMap<>();
        for (Object[] row : emailLogRepo.countGroupedByStatus(userId)) {
            EmailStatus status = (EmailStatus) row[0];
            Long count = (Long) row[1];
            statusCounts.put(status.name(), count);
        }

        Long totalOpens = emailLogRepo.totalOpenCount(userId);
        Long totalClicks = emailLogRepo.totalClickCount(userId);

        var summary = new EmailMetricsDto.MetricsSummary(
                emailLogRepo.countByUserId(userId),
                statusCounts.getOrDefault("PENDING", 0L),
                statusCounts.getOrDefault("SENT", 0L),
                statusCounts.getOrDefault("DELIVERED", 0L),
                statusCounts.getOrDefault("FAILED", 0L),
                statusCounts.getOrDefault("BOUNCED", 0L),
                statusCounts.getOrDefault("SUPPRESSED", 0L),
                totalOpens != null ? totalOpens : 0L,
                totalClicks != null ? totalClicks : 0L
        );

        // Daily breakdown
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        List<EmailMetricsDto.DailyCount> daily = emailLogRepo.dailyCountsSince(userId, since)
                .stream()
                .map(row -> new EmailMetricsDto.DailyCount(
                        row[0].toString(),
                        row[1].toString(),
                        ((Number) row[2]).longValue()))
                .toList();

        return ResponseEntity.ok(new EmailMetricsDto.MetricsResponse(summary, daily));
    }

}
