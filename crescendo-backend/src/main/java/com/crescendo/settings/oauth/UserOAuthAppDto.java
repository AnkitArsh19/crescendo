package com.crescendo.settings.oauth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * DTOs for the user OAuth app settings endpoints.
 */
public class UserOAuthAppDto {

    /**
     * Request to save (create or update) a custom OAuth app config.
     */
    public record SaveOAuthAppRequest(
            /** Provider key, e.g. "slack", "github", "gmail". */
            @NotBlank @Size(max = 100) String providerKey,
            /** OAuth client ID from the provider's developer console. */
            @NotBlank String clientId,
            /** OAuth client secret from the provider's developer console. */
            @NotBlank String clientSecret,
            /**
             * Space-separated scopes to request.
             * Leave blank to use the platform-level defaults.
             */
            String scopes
    ) {}

    /**
     * Safe summary returned in the list endpoint.
     * Secrets are never returned.
     */
    public record OAuthAppSummary(
            String providerKey,
            String scopes,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt
    ) {}

    /**
     * Internal DTO — decrypted credentials for use by the OAuth authorization flow.
     * Never serialized to JSON or returned to the client.
     */
    public record DecryptedOAuthApp(
            String clientId,
            String clientSecret,
            String scopes
    ) {}
}
