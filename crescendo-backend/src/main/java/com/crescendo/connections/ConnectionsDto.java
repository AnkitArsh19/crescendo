package com.crescendo.connections;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Request and response DTOs for connection operations.
 *
 * Connections store user credentials for third-party apps (OAuth2 tokens, API keys, etc.).
 * The query-side projection intentionally excludes credential data — secrets never leave
 * the command database through read endpoints.
 */
public class ConnectionsDto {

    // REQUESTS

    public record CreateConnectionRequest(
            @NotBlank @Size(max = 100) String appKey,
            @NotBlank @Size(max = 255) String name,
            @NotNull Map<String, Object> credentials
    ) {}

    public record UpdateConnectionRequest(
            @Size(min = 1, max = 255) String name,
            Map<String, Object> credentials
    ) {}

    // RESPONSES

    /**
     * Read-safe connection summary — no credentials exposed.
     */
    public record ConnectionResponse(
            UUID id,
            String appKey,
            String name,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
