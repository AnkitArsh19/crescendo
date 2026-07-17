package com.crescendo.user;

import com.crescendo.enums.AuthProvider;
import com.crescendo.security.AppUserDetails;
import com.crescendo.security.JWTService;
import com.crescendo.user.user_command.User_commandService;
import com.crescendo.user.user_query.User_queryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Account-management endpoints under /users/me.
 *
 * All endpoints require a valid Bearer JWT — the authenticated user's ID is
 * extracted from the security context, so a user can only operate on their own account.
 *
 * Read operations:
 *   GET  /users/me                        — full profile (email, username, role, providers, MFA, sessions)
 *
 * Write operations:
 *   PATCH  /users/me                      — update display username
 *   POST   /users/me/password             — set a brand-new password (OAuth-only account)
 *   POST   /users/me/unlink-provider      — detach a linked OAuth provider
 *   DELETE /users/me/sessions/{sessionId} — revoke a single session (remote logout)
 *   POST   /users/me/sessions/revoke-others — revoke all sessions except the current one
 *   DELETE /users/me                      — permanently delete the account
 */
@RestController
@RequestMapping("/users/me")
public class UserController {

    private final User_queryService queryService;
    private final User_commandService commandService;
    private final JWTService jwtService;

    public UserController(User_queryService queryService,
                          User_commandService commandService,
                          JWTService jwtService) {
        this.queryService = queryService;
        this.commandService = commandService;
        this.jwtService = jwtService;
    }

    // PROFILE

    /**
     * GET /users/me
     * Returns the full account view: profile, linked providers, MFA status, active sessions.
     */
    @GetMapping
    public ResponseEntity<UserDto.AccountResponse> getAccount(Authentication auth,
                                                              HttpServletRequest request) {
        UUID userId = principal(auth).getId();
        UUID sessionId = extractSessionId(request);
        return ResponseEntity.ok(queryService.getAccount(userId, sessionId));
    }

    /**
     * PATCH /users/me
     * Updates the display username.  409 if the new username is already taken.
     */
    @PatchMapping
    public ResponseEntity<Map<String, String>> updateProfile(@Valid @RequestBody UserDto.UpdateProfileRequest req,
                                                             Authentication auth) {
        commandService.updateUsername(principal(auth).getId(), req);
        return ResponseEntity.ok(Map.of("message", "Profile updated"));
    }

    // PASSWORD

    /**
     * POST /users/me/password
     * Sets a password for an OAuth-only account (no existing local credential).
     * After this the user can log in with email + password in addition to OAuth.
     * Returns 409 if a password already exists (use PATCH /auth/change-password instead).
     */
    @PostMapping("/password")
    public ResponseEntity<Map<String, String>> setPassword(@Valid @RequestBody UserDto.SetPasswordRequest req,
                                                           Authentication auth) {
        commandService.setPassword(principal(auth).getId(), req);
        return ResponseEntity.ok(Map.of("message", "Password set successfully"));
    }

    // LINKED PROVIDERS

    /**
     * POST /users/me/unlink-provider
     * Detaches a linked OAuth provider (GOOGLE, GITHUB, etc.) from the account.
     * Returns 400 if unlinking would leave the user with zero login methods.
     */
    @PostMapping("/unlink-provider")
    public ResponseEntity<Map<String, String>> unlinkProvider(@Valid @RequestBody UserDto.UnlinkProviderRequest req,
                                                              Authentication auth) {
        commandService.unlinkProvider(principal(auth).getId(), req);
        return ResponseEntity.ok(Map.of("message", req.provider().name() + " unlinked"));
    }

    /**
     * POST /users/me/link-provider/init
     * Prepares a provider-linking flow by setting a short-lived HttpOnly cookie with the
     * authenticated user's ID. After this call returns, the frontend redirects to the standard
     * OAuth2 authorization URL. The OAuth2LoginSuccessHandler detects the cookie and adds the
     * new provider identity to the EXISTING account instead of creating a new login session.
     *
     * This is necessary because the OAuth redirect flow leaves the SPA context — the JWT
     * in Zustand memory is lost — so we pass the user ID via a secure cookie.
     */
    @PostMapping("/link-provider/init")
    public ResponseEntity<Map<String, String>> initLinkProvider(
            @RequestBody Map<String, String> body,
            Authentication auth,
            HttpServletResponse response) {

        String providerStr = body.get("provider");
        if (providerStr == null || providerStr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "provider is required");
        }

        // Validate that the provider is a known enum value.
        AuthProvider provider;
        try {
            provider = AuthProvider.valueOf(providerStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown provider: " + providerStr);
        }

        UUID userId = principal(auth).getId();

        // Set a short-lived cookie that the OAuth2LoginSuccessHandler will read.
        // 5-minute TTL is generous — the OAuth redirect round-trip should complete in seconds.
        // HttpOnly=true prevents JavaScript from reading the user ID.
        ResponseCookie cookie = ResponseCookie.from("link_user_id", userId.toString())
                .httpOnly(true)
                .secure(false)  // false for local dev (HTTP); set true in production
                .path("/")
                .sameSite("Lax") // Lax allows the cookie to be sent on OAuth redirect back
                .maxAge(Duration.ofMinutes(5))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok(Map.of(
                "message", "Ready to link " + provider.name(),
                "redirectUrl", "/oauth2/authorization/" + provider.name().toLowerCase()
        ));
    }

    // SESSIONS

    /**
     * DELETE /users/me/sessions/{sessionId}
     * Revokes a specific session.  The frontend shows each active session
     * (device label, user-agent, IP) and lets the user revoke any of them.
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, String>> revokeSession(@PathVariable UUID sessionId,
                                                             Authentication auth) {
        commandService.revokeSession(principal(auth).getId(), sessionId);
        return ResponseEntity.ok(Map.of("message", "Session revoked"));
    }

    /**
     * POST /users/me/sessions/revoke-others
     * "Log out everywhere else" — revokes every active session except the current one.
     */
    @PostMapping("/sessions/revoke-others")
    public ResponseEntity<Map<String, String>> revokeOtherSessions(Authentication auth,
                                                                   HttpServletRequest request) {
        UUID currentSessionId = extractSessionId(request);
        commandService.revokeAllOtherSessions(principal(auth).getId(), currentSessionId);
        return ResponseEntity.ok(Map.of("message", "All other sessions revoked"));
    }

    // PASSKEY NUDGE

    /**
     * POST /users/me/passkey-nudge/dismiss
     * Records a passkey-setup nudge dismissal for the authenticated user.
     *
     * Body: { "permanent": false }   → temporary X-close (increments dismiss count, enforces 14-day cooldown and 2-strike max)
     * Body: { "permanent": true }    → explicit "Don't ask again" (sets optedOut flag, nudge never shown again)
     *
     * The backend stores this so throttling is per-account across devices, not per-browser.
     */
    @PostMapping("/passkey-nudge/dismiss")
    public ResponseEntity<Map<String, String>> dismissPasskeyNudge(
            @RequestBody Map<String, Boolean> body,
            Authentication auth) {
        boolean permanent = Boolean.TRUE.equals(body.get("permanent"));
        commandService.dismissPasskeyNudge(principal(auth).getId(), permanent);
        return ResponseEntity.ok(Map.of("message", permanent ? "Opted out of passkey nudge" : "Nudge dismissed"));
    }

    // ACCOUNT DELETION


    /**
     * DELETE /users/me
     * Permanently deletes the account and all associated data (credentials, identities,
     * MFA settings, backup codes, sessions).  This action is irreversible.
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(Authentication auth) {
        commandService.deleteAccount(principal(auth).getId());
        return ResponseEntity.noContent().build();
    }

    // HELPERS

    /// Extracts the AppUserDetails principal from the Authentication object.
    private AppUserDetails principal(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails details))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        return details;
    }

    /// Resolves the JWT from the Authorization header and extracts the session ID claim.
    /// Falls back to null if the token or claim is absent (shouldn't happen for authenticated requests).
    private UUID extractSessionId(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String sid = jwtService.extractSessionId(header.substring(7));
            if (sid != null) return UUID.fromString(sid);
        }
        return null;
    }
}
