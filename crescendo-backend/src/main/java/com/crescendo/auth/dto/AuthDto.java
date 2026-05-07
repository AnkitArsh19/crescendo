package com.crescendo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * Consolidated authentication DTO container mirroring the style of {@code MFADto}.
 * Each nested record represents a request or response payload used by auth flows.
 */
public class AuthDto {

    /** Login using email + password. */
    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password,
            boolean rememberMe
    ) {}

    /** Returned after successful login (password or OAuth) or initial registration auto-login. */
    public record LoginResponse(
            String userId,
            String email,
            String username,
            String role,
            List<String> providers,
            boolean hasLocalCredential,
            String accessToken,
            Instant accessExpiresAt,
            String refreshToken,
            Instant refreshExpiresAt,
            Instant createdAt
    ) {}

    /** Registration request for local credential signup. */
    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 1, max = 100) String username,
            @NotBlank @Size(min = 6) String password
    ) {}

    /** Returned after successful registration (and optional auto-login issuing tokens). */
    public record RegisterResponse(
            String userId,
            String email,
            String username,
            String role,
            List<String> providers,
            boolean hasLocalCredential,
            String accessToken,
            Instant accessExpiresAt,
            String refreshToken,
            Instant refreshExpiresAt,
            String message,
            Instant createdAt
    ) {}

    /** Initiates password reset by email. */
    public record ForgotPasswordRequest(
            @Email @NotBlank String email
    ) {}

    /** Completing a password reset using a reset token emailed to the user. */
    public record ResetPasswordRequest(
            @NotBlank String resetToken,
            @NotBlank @Size(min = 6) String newPassword
    ) {}

    /** Authenticated user changing their password (old -> new). */
    public record PasswordChangeRequest(
            @NotBlank String oldPassword,
            @NotBlank @Size(min = 6) String newPassword
    ) {}

    /**
     * Returned from POST /auth/login when MFA is enabled.
     * The client must next call POST /mfa/challenge with their TOTP code.
     * No tokens are issued at this stage — issuing tokens before MFA passes would defeat the point.
     */
    public record MfaRequiredResponse(boolean mfaRequired) {}

    /** Client submits refresh token to obtain a new access token (and possibly rotated refresh token). */
    public record RefreshTokenRequest(
            @NotBlank String refreshToken
    ) {}

    /** Response for a refresh operation (may include rotated refresh token & expiry). */
    public record AccessTokenResponse(
            String accessToken,
            Instant accessExpiresAt,
            String refreshToken,
            Instant refreshExpiresAt
    ) {}
}
