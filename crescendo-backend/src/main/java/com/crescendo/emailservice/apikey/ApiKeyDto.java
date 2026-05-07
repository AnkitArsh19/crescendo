package com.crescendo.emailservice.apikey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class ApiKeyDto {

    private ApiKeyDto() {}

    public record CreateApiKeyRequest(
            @NotBlank @Size(min = 1, max = 255) String name
    ) {}

    /// Returned only on creation — contains the plain key shown ONCE.
    /// The key cannot be retrieved again after this response.
    public record ApiKeyCreatedResponse(
            UUID id,
            String name,
            String prefix,
            String plainKey
    ) {}

    /// Standard response for listing/getting API keys — never contains the raw key.
    public record ApiKeyResponse(
            UUID id,
            String name,
            String prefix,
            Instant createdAt,
            Instant lastUsedAt
    ) {}
}
