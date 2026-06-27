package com.crescendo.publicapi.oauth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DeveloperApplicationDto {
    private DeveloperApplicationDto() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 1000) String logoUrl,
            boolean publicClient,
            @NotEmpty @Size(max = 10) List<@NotBlank @Size(max = 1000) String> redirectUris,
            @NotEmpty List<@NotBlank String> scopes
    ) {
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 1000) String logoUrl,
            @NotEmpty @Size(max = 10) List<@NotBlank @Size(max = 1000) String> redirectUris,
            @NotEmpty List<@NotBlank String> scopes
    ) {
    }

    public record ApplicationResponse(
            String id,
            String clientId,
            String name,
            String logoUrl,
            boolean publicClient,
            boolean active,
            int rateLimitPerMinute,
            List<String> redirectUris,
            List<String> scopes,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record CreatedResponse(
            ApplicationResponse application,
            String clientSecret
    ) {
    }

    public record SecretResponse(
            String clientSecret
    ) {
    }

    public record UsageResponse(
            UUID id,
            String authorizationId,
            UUID userId,
            String method,
            String path,
            int status,
            String ipAddress,
            String userAgent,
            Instant createdAt
    ) {
    }
}
