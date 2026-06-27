package com.crescendo.settings.oauth;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.crescendo.security.AuthenticatedUser.userId;

/**
 * REST endpoints for managing per-user custom OAuth app configurations.
 *
 * <p>
 * All endpoints require authentication. The authenticated user's ID is
 * extracted via
 * {@link com.crescendo.security.AuthenticatedUser#userId(Authentication)}.
 *
 * <pre>
 * GET    /settings/oauth-apps                — list configured providers
 * POST   /settings/oauth-apps               — save (upsert) a config
 * DELETE /settings/oauth-apps/{providerKey} — delete a config
 * </pre>
 */
@RestController
@RequestMapping("/settings/oauth-apps")
public class UserOAuthAppController {

    private final UserOAuthAppService service;

    public UserOAuthAppController(UserOAuthAppService service) {
        this.service = service;
    }

    /**
     * Lists all custom OAuth app configs for the authenticated user.
     * Client secrets are never returned.
     */
    @GetMapping
    public ResponseEntity<List<UserOAuthAppDto.OAuthAppSummary>> list(Authentication auth) {
        return ResponseEntity.ok(service.list(userId(auth)));
    }

    /**
     * Saves (create or update) a custom OAuth app config for a provider.
     */
    @PostMapping
    public ResponseEntity<Void> save(
            Authentication auth,
            @Valid @RequestBody UserOAuthAppDto.SaveOAuthAppRequest req) {
        service.save(userId(auth), req);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes the custom OAuth app config for a specific provider.
     */
    @DeleteMapping("/{providerKey}")
    public ResponseEntity<Void> delete(
            Authentication auth,
            @PathVariable String providerKey) {
        service.delete(userId(auth), providerKey);
        return ResponseEntity.noContent().build();
    }
}
