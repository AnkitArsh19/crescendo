package com.crescendo.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionDto(
        UUID id,
        String deviceLabel,
        String clientIp,
        String country,
        Instant createdAt,
        Instant lastUsedAt,
        boolean isCurrent
) {}
