package com.crescendo.connections.oauth;

import com.crescendo.security.AppUserDetails;
import com.crescendo.settings.oauth.UserOAuthAppDto;
import com.crescendo.settings.oauth.UserOAuthAppService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.net.URI;
import java.util.Map;

/**
 * REST controller for integration-level OAuth connect flows.
 *
 * <p>These endpoints are NOT for user login/registration — they are for connecting
 * third-party apps (Gmail, Notion, Slack, etc.) to the user's Crescendo account.
 * The user must already be authenticated.
 *
 * <pre>
 *   GET /connections/oauth/{provider}/authorize    → returns the authorization URL
 *   GET /connections/oauth/{provider}/callback     → handles provider callback, creates connection, redirects to frontend
 * </pre>
 */
@RestController
@RequestMapping("/connections/oauth")
public class IntegrationOAuthController {

    private final IntegrationOAuthService oauthService;
    private final UserOAuthAppService userOAuthAppService;

    public IntegrationOAuthController(IntegrationOAuthService oauthService,
                                      UserOAuthAppService userOAuthAppService) {
        this.oauthService = oauthService;
        this.userOAuthAppService = userOAuthAppService;
    }

    /**
     * Returns the OAuth authorization URL for the given provider.
     * Frontend opens this URL in a popup window.
     *
     * <p>Accepts an optional body with custom OAuth app credentials.
     * If {@code customClientId} and {@code customClientSecret} are provided,
     * a {@link com.crescendo.settings.oauth.UserOAuthApp} record is implicitly
     * upserted so the user's OAuth app is reused on future connections — without
     * requiring a separate trip to Settings.
     *
     * <p>Changed from GET to POST to accept a request body.
     */
    @PostMapping("/{provider}/authorize")
    public ResponseEntity<Map<String, String>> authorize(
            @PathVariable String provider,
            @RequestBody(required = false) AuthorizeRequest body,
            @AuthenticationPrincipal AppUserDetails principal) {

        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        String connectionId = body != null ? body.connectionId() : null;

        // ── Implicit UserOAuthApp upsert ──────────────────────────────────────
        // If the user filled in their own client_id/secret in the connection form,
        // save (or update) their custom OAuth app for this provider now.
        // This avoids a mandatory detour to Settings before connecting.
        if (body != null
                && body.customClientId() != null && !body.customClientId().isBlank()
                && body.customClientSecret() != null && !body.customClientSecret().isBlank()) {
            userOAuthAppService.save(principal.getId(), new UserOAuthAppDto.SaveOAuthAppRequest(
                    provider,
                    body.customClientId(),
                    body.customClientSecret(),
                    body.customScopes()
            ));
        }

        String authUrl = oauthService.buildAuthorizationUrl(provider, principal.getId(), connectionId);
        return ResponseEntity.ok(Map.of("authorizationUrl", authUrl));
    }

    /**
     * Request body for POST /authorize.
     * All fields are optional — omitting customClientId/Secret means use platform defaults.
     */
    public record AuthorizeRequest(
            /** Existing connection ID for reconnect flows; null for new connections. */
            String connectionId,
            /** User-provided OAuth client ID (overrides platform default). */
            String customClientId,
            /** User-provided OAuth client secret (overrides platform default). */
            String customClientSecret,
            /** Optional custom scopes (space-separated). Null = use provider defaults. */
            String customScopes
    ) {}

    /**
     * Handles the OAuth callback from the provider.
     * Exchanges the authorization code for tokens and creates the connection.
     * This endpoint is permitAll (the state parameter carries user identity).
     */
    @GetMapping("/{provider}/callback")
    public ResponseEntity<Void> callback(
            @PathVariable String provider,
            @RequestParam("code") String code,
            @RequestParam("state") String state) {

        String redirectUrl = oauthService.handleCallback(provider, code, state);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }
}
