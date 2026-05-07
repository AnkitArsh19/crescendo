package com.crescendo.user;

import com.crescendo.enums.AuthProvider;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * All request and response DTOs for the /users/me account-management endpoints.
 *
 *  Read operations (query side):
 *    AccountResponse          — full profile view (email, username, role, providers, MFA, sessions)
 *    LinkedAccountResponse    — one linked OAuth provider
 *    ActiveSessionResponse    — one active refresh-token session
 *    MfaStatusResponse        — whether MFA is enrolled / enabled
 *
 *  Write operations (command side):
 *    UpdateProfileRequest     — change username
 *    SetPasswordRequest       — set a brand-new password (OAuth-only user wants to add local login)
 *    UnlinkProviderRequest    — remove a linked OAuth provider
 */
public class UserDto {

    // ────────────────────────────── RESPONSES ──────────────────────────────

    /// Full profile returned by GET /users/me.
    /// Aggregates data from User_command, UserCredential, UserIdentity, UserMFASetting, and UserSession.
    public record AccountResponse(
            String userId,
            String email,
            String username,
            String role,
            boolean hasLocalCredential,
            MfaStatusResponse mfa,
            List<LinkedAccountResponse> linkedAccounts,
            List<ActiveSessionResponse> activeSessions,
            Instant createdAt,
            LimitsResponse limits
    ) {}

    /// The effective access tier and plan limits for this account.
    /// maxWorkflows / maxConnections are -1 for unlimited (ADMIN).
    /// "limited" is true for UNVERIFIED accounts whose email is not yet confirmed.
    public record LimitsResponse(
            String tier,
            boolean limited,
            int maxWorkflows,
            int maxConnections
    ) {}

    /// One linked OAuth provider (Google, GitHub, etc.).
    /// The email field is the email the OAuth provider reported at the time of linking —
    /// it may differ from the canonical account email if the user changed it later.
    public record LinkedAccountResponse(
            String id,
            String provider,
            String providerEmail,
            Instant linkedAt
    ) {}

    /// One active refresh-token session.
    /// "current" is true if this session matches the caller's own JWT session ID.
    public record ActiveSessionResponse(
            String sessionId,
            String userAgent,
            String clientIp,
            String deviceLabel,
            boolean current,
            Instant createdAt,
            Instant lastUsedAt,
            Instant expiresAt
    ) {}

    /// MFA enrollment/enablement status.
    public record MfaStatusResponse(
            boolean enrolled,
            boolean enabled
    ) {}

    /// Update the display username.
    public record UpdateProfileRequest(
            @NotBlank @Size(min = 1, max = 100) String username
    ) {}

    /// Set a brand-new password for an OAuth-only account that has no local credential yet.
    /// This does NOT replace an existing password — use PATCH /auth/change-password for that.
    public record SetPasswordRequest(
            @NotBlank @Size(min = 6) String password
    ) {}

    /// Which OAuth provider to unlink from the account.
    public record UnlinkProviderRequest(
            @NotNull AuthProvider provider
    ) {}
}
