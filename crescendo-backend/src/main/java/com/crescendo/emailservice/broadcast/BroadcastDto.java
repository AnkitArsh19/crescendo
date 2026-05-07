package com.crescendo.emailservice.broadcast;

import com.crescendo.enums.BroadcastStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class BroadcastDto {

    private BroadcastDto() {}

    public record CreateBroadcastRequest(
            @NotNull UUID templateId,
            @NotBlank @Email String fromAddress
    ) {}

    public record BroadcastResponse(
            UUID id,
            UUID templateId,
            String fromAddress,
            BroadcastStatus status,
            int totalCount,
            int sentCount,
            int failedCount,
            String error,
            Instant createdAt,
            Instant startedAt,
            Instant completedAt
    ) {}
}
