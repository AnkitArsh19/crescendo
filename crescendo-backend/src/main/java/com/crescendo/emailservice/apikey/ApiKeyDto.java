package com.crescendo.emailservice.apikey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ApiKeyDto {

    private ApiKeyDto() {}

    public record CreateApiKeyRequest(
            @NotBlank @Size(min = 1, max = 255) String name,
            List<String> scopes,
            @Min(1) @Max(10_000) Integer rateLimitPerMinute,
            @Min(1) @Max(365) Integer expiresInDays
    ) {}

    public record RotateApiKeyRequest(
            @Min(0) @Max(168) Integer gracePeriodHours
    ) {}

    /// Returned only on creation — contains the plain key shown ONCE.
    /// The key cannot be retrieved again after this response.
    public record ApiKeyCreatedResponse(
            UUID id,
            String name,
            String prefix,
            String plainKey,
            List<String> scopes,
            int rateLimitPerMinute,
            Instant expiresAt
    ) {}

    /// Standard response for listing/getting API keys — never contains the raw key.
    public record ApiKeyResponse(
            UUID id,
            String name,
            String prefix,
            List<String> scopes,
            int rateLimitPerMinute,
            Instant createdAt,
            Instant lastUsedAt,
            Instant expiresAt,
            Instant rotationGraceEndsAt,
            String status
    ) {}

    public record ApiKeyUsageResponse(
            UUID id,
            UUID apiKeyId,
            String method,
            String path,
            int status,
            String ipAddress,
            String userAgent,
            Instant createdAt
    ) {}
}
