package com.crescendo.publicapi.email;

import java.util.List;

public class PublicEmailMetricsDto {

    public record MetricsSummary(
            long total,
            long pending,
            long sent,
            long delivered,
            long failed,
            long bounced,
            long suppressed,
            long totalOpens,
            long totalClicks
    ) {}

    public record DailyCount(
            String date,
            String status,
            long count
    ) {}

    public record MetricsResponse(
            MetricsSummary summary,
            List<DailyCount> daily
    ) {}
}
