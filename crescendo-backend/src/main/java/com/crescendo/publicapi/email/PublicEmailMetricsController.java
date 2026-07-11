package com.crescendo.publicapi.email;

import com.crescendo.emailservice.email_log.EmailLogRepository;
import com.crescendo.enums.EmailStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.crescendo.security.AuthenticatedUser.userId;
import static com.crescendo.publicapi.PublicApiScopes.METRICS_READ;
import static com.crescendo.publicapi.PublicApiScopes.require;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/metrics")
@Tag(name = "Metrics", description = "Public API for email delivery metrics and analytics")
public class PublicEmailMetricsController {

    private final EmailLogRepository emailLogRepo;

    public PublicEmailMetricsController(EmailLogRepository emailLogRepo) {
        this.emailLogRepo = emailLogRepo;
    }

    @GetMapping
    public ResponseEntity<PublicEmailMetricsDto.MetricsResponse> getMetrics(
            @RequestParam(defaultValue = "30") int days,
            Authentication auth) {
        require(auth, METRICS_READ);
        UUID userId = userId(auth);

        long total = emailLogRepo.countByUserId(userId);

        List<Object[]> statusCounts = emailLogRepo.countGroupedByStatus(userId);
        long pending = 0, sent = 0, delivered = 0, failed = 0, bounced = 0, suppressed = 0;

        for (Object[] row : statusCounts) {
            EmailStatus status = (EmailStatus) row[0];
            long count = ((Number) row[1]).longValue();
            switch (status) {
                case PENDING -> pending = count;
                case SENT -> sent = count;
                case DELIVERED -> delivered = count;
                case FAILED -> failed = count;
                case BOUNCED -> bounced = count;
                case SUPPRESSED -> suppressed = count;
                case COMPLAINED -> {
                } // ignore or group
            }
        }

        Long totalOpens = emailLogRepo.totalOpenCount(userId);
        Long totalClicks = emailLogRepo.totalClickCount(userId);

        PublicEmailMetricsDto.MetricsSummary summary = new PublicEmailMetricsDto.MetricsSummary(
                total, pending, sent, delivered, failed, bounced, suppressed,
                totalOpens != null ? totalOpens : 0,
                totalClicks != null ? totalClicks : 0);

        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        List<Object[]> dailyData = emailLogRepo.dailyCountsSince(userId, since);
        List<PublicEmailMetricsDto.DailyCount> daily = new ArrayList<>();

        for (Object[] row : dailyData) {
            daily.add(new PublicEmailMetricsDto.DailyCount(
                    row[0].toString(),
                    row[1].toString(),
                    ((Number) row[2]).longValue()));
        }

        return ResponseEntity.ok(new PublicEmailMetricsDto.MetricsResponse(summary, daily));
    }
}
