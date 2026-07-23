package com.crescendo.execution.resource;

import java.time.Duration;

/** Metadata owned by a connector for safe workflow-planning resource snapshots. */
public record ResourceContextDescriptor(String resourceType, int maxItems, Duration cacheTtl) {
    public ResourceContextDescriptor {
        if (maxItems < 1 || maxItems > 200) throw new IllegalArgumentException("maxItems must be between 1 and 200");
        if (cacheTtl == null || cacheTtl.isNegative() || cacheTtl.isZero()) cacheTtl = Duration.ofMinutes(5);
    }
}
