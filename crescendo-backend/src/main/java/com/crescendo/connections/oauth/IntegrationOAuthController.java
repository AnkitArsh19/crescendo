package com.crescendo.connections.oauth;

import com.crescendo.security.AppUserDetails;
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

    public IntegrationOAuthController(IntegrationOAuthService oauthService) {
        this.oauthService = oauthService;
    }

    /**
     * Returns the OAuth authorization URL for the given provider.
     * Frontend opens this URL in a popup or redirects the user.
     */
    @GetMapping("/{provider}/authorize")
    public ResponseEntity<Map<String, String>> authorize(
            @PathVariable String provider,
            @AuthenticationPrincipal AppUserDetails principal) {

        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        String authUrl = oauthService.buildAuthorizationUrl(provider, principal.getId());
        return ResponseEntity.ok(Map.of("authorizationUrl", authUrl));
    }

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
