package com.crescendo.auth.controller;

import com.crescendo.auth.dto.AuthDto;
import com.crescendo.auth.service.AuthenticationService;
import com.crescendo.security.AppUserDetails;
import com.crescendo.security.RefreshTokenCookieService;
import com.crescendo.security.mfa.MFAService;
import com.crescendo.user.user_command.User_command;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

/**
 * All authentication endpoints live under /auth.
 *
 * Public endpoints (no JWT required):
 *   POST /auth/register         — local email+password sign-up
 *   POST /auth/login            — local login (returns 202 + mfaRequired=true if MFA is enabled)
 *   POST /auth/refresh          — rotate access token using refresh cookie or request body
 *   POST /auth/forgot-password  — send password-reset email
 *   POST /auth/reset-password   — consume reset token and set new password
 *   POST /auth/verify-email     — consume email-verification link
 *
 * Authenticated endpoints (Bearer JWT required):
 *   POST  /auth/logout                — revoke refresh token + clear cookie
 *   PATCH /auth/change-password       — old-password + new-password
 *   POST  /auth/resend-verification   — send a new verification email
 */
@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService authService;
    private final MFAService mfaService;
    private final RefreshTokenCookieService cookieService;

    /// Controls whether the refresh-token cookie carries the Secure flag.
    /// Set to false in local development (HTTP), true in all deployed environments (HTTPS).
    @Value("${app.cookie.secure:false}")
    private boolean secureCookie;

    /// Matches jwt.refresh.expiration — used to compute the cookie maxAge.
    @Value("${jwt.refresh.expiration}")
    private long refreshExpirationMs;

    public AuthenticationController(AuthenticationService authService,
                                    MFAService mfaService,
                                    RefreshTokenCookieService cookieService) {
        this.authService = authService;
        this.mfaService = mfaService;
        this.cookieService = cookieService;
    }

    // REGISTRATION
    /**
     * POST /auth/register
     * Creates a new local-credential account and returns tokens on success (auto-login).
     * 201 Created on success, 409 Conflict if email or username is already taken.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthDto.RegisterResponse> register(
            @Valid @RequestBody AuthDto.RegisterRequest req,
            HttpServletRequest servReq,
            HttpServletResponse servRes) {

        AuthDto.RegisterResponse resp = authService.register(req, userAgent(servReq));
        // Set refresh token in HttpOnly cookie so JS cannot read it.
        setRefreshCookie(servRes, resp.refreshToken(), resp.refreshExpiresAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    // LOCAL LOGIN
    /**
     * POST /auth/login
     * Two possible outcomes:
     *   200 OK   — credentials valid, MFA not enabled → access + refresh tokens returned.
     *   202 Accepted — credentials valid, MFA IS enabled → {mfaRequired:true} returned,
     *                  client must next call POST /mfa/challenge with their TOTP code.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody AuthDto.LoginRequest req,
            HttpServletRequest servReq,
            HttpServletResponse servRes) {

        // Verify credentials first without issuing tokens — MFA check happens in between.
        User_command user = authService.verifyLocalCredentials(req);

        // If MFA is active, refuse to issue tokens now — the client must complete the TOTP step.
        if (mfaService.hasEnabledMfa(user.getId())) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new AuthDto.MfaRequiredResponse(true));
        }

        AuthDto.LoginResponse resp = authService.issueLoginResponse(user, userAgent(servReq), req.rememberMe());
        setRefreshCookie(servRes, resp.refreshToken(), resp.refreshExpiresAt());
        return ResponseEntity.ok(resp);
    }

    // TOKEN REFRESH
    /**
     * POST /auth/refresh
     * Reads the refresh token from the HttpOnly cookie first; falls back to the request body
     * so the endpoint works for clients that don't support cookies (e.g. mobile apps).
     * Returns a new access token (and a rotated refresh token if jwt.refresh.rotate=true).
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthDto.AccessTokenResponse> refresh(
            @RequestBody(required = false) AuthDto.RefreshTokenRequest body,
            HttpServletRequest servReq,
            HttpServletResponse servRes) {

        String rawToken = extractRefreshCookie(servReq);
        if (rawToken == null && body != null) rawToken = body.refreshToken();
        if (rawToken == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No refresh token provided");

        AuthDto.AccessTokenResponse resp = authService.refreshTokens(rawToken, userAgent(servReq));

        // If a new refresh token was rotated in, update the cookie too.
        if (resp.refreshToken() != null) {
            setRefreshCookie(servRes, resp.refreshToken(), resp.refreshExpiresAt());
        }
        return ResponseEntity.ok(resp);
    }

    // LOGOUT
    /**
     * POST /auth/logout
     * Revokes the session (deletes UserSession row) and clears the refresh-token cookie.
     * Authenticated endpoint — the JWT filter ensures the caller is logged in.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest servReq, HttpServletResponse servRes) {
        String rawToken = extractRefreshCookie(servReq);
        if (rawToken == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No refresh token cookie present");

        authService.logout(rawToken);
        // Clear the cookie regardless of whether the token was found — ensures the browser drops it.
        cookieService.clear(servRes, secureCookie);
        return ResponseEntity.noContent().build();
    }

    // PASSWORD RESET
    /**
     * POST /auth/forgot-password
     * Always returns 204 even if the email is not registered (prevents account enumeration).
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody AuthDto.ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /auth/reset-password
     * Consumes the one-time reset token (sent by email) and sets a new password.
     * 400 if the token is expired, already used, or invalid.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody AuthDto.ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /auth/change-password
     * Authenticated — allows a logged-in user to change their password after verifying the old one.
     * 400 if the account uses OAuth only (no local credential).
     */
    @PatchMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody AuthDto.PasswordChangeRequest req,
            Authentication auth) {

        authService.changePassword(currentUserId(auth), req);
        return ResponseEntity.noContent().build();
    }

    // EMAIL VERIFICATION
    /**
     * POST /auth/verify-email?token={raw}
     * Consumes the email verification link sent after registration.
     * 400 if expired, already used, or invalid.
     */
    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /auth/resend-verification
     * Authenticated — sends a fresh verification email to the currently logged-in user.
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(Authentication auth) {
        authService.sendVerificationEmail(currentUserId(auth));
        return ResponseEntity.noContent().build();
    }

    /// Extracts the User-Agent header for session fingerprinting (stored in UserSession).
    private String userAgent(HttpServletRequest req) {
        return req.getHeader("User-Agent");
    }

    /// Reads the refresh_token HttpOnly cookie from the incoming request.
    /// Returns null if absent so callers can fall back to the request body.
    private String extractRefreshCookie(HttpServletRequest req) {
        if (req.getCookies() == null) return null;
        return Arrays.stream(req.getCookies())
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /// Writes the refresh token as an HttpOnly cookie.
    /// TTL is computed from expiresAt so the cookie and the DB row expire at exactly the same time.
    private void setRefreshCookie(HttpServletResponse res, String token, Instant expiresAt) {
        long ttlMs = Duration.between(Instant.now(), expiresAt).toMillis();
        cookieService.setRefreshToken(res, token, ttlMs, secureCookie);
    }

    /// Extracts the authenticated user's UUID from the Spring Security context.
    /// AppUserDetails is always the principal type when a valid JWT is present.
    private UUID currentUserId(Authentication auth) {
        return ((AppUserDetails) auth.getPrincipal()).getId();
    }
}
