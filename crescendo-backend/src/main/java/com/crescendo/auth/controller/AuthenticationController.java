package com.crescendo.auth.controller;

import com.crescendo.auth.dto.AuthDto;
import com.crescendo.auth.service.AuthenticationService;
import com.crescendo.auth.token.DesktopHandoffService;
import com.crescendo.security.AppUserDetails;
import com.crescendo.security.RefreshTokenCookieService;
import com.crescendo.security.TokenPair;
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
 *   POST /auth/register                      — local email+password sign-up
 *   POST /auth/login                         — local login (returns 202 + mfaRequired=true if MFA enabled)
 *   POST /auth/refresh                       — rotate access token using refresh cookie or request body
 *   POST /auth/forgot-password               — send password-reset email
 *   POST /auth/reset-password                — consume reset token and set new password
 *   POST /auth/verify-email                  — consume email-verification link
 *   POST /auth/desktop-handoff/exchange      — exchange a one-time handoff code for a token pair (RFC 8252)
 *
 * Authenticated endpoints (Bearer JWT required):
 *   POST  /auth/logout                       — revoke refresh token + clear cookie
 *   PATCH /auth/change-password              — old-password + new-password
 *   POST  /auth/resend-verification          — send a new verification email
 *   POST  /auth/desktop-handoff/issue        — issue a one-time desktop handoff code (RFC 8252)
 */
@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService authService;
    private final MFAService mfaService;
    private final RefreshTokenCookieService cookieService;
    private final DesktopHandoffService desktopHandoffService;

    /// Controls whether the refresh-token cookie carries the Secure flag.
    /// Set to false in local development (HTTP), true in all deployed environments (HTTPS).
    @Value("${app.cookie.secure:false}")
    private boolean secureCookie;

    /// Matches AuthRateLimitingFilter — only honor X-Forwarded-For when running behind a trusted proxy.
    @Value("${app.security.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;

    /// Matches jwt.refresh.expiration — used to compute the cookie maxAge.
    @Value("${jwt.refresh.expiration}")
    private long refreshExpirationMs;

    public AuthenticationController(AuthenticationService authService,
                                    MFAService mfaService,
                                    RefreshTokenCookieService cookieService,
                                    DesktopHandoffService desktopHandoffService) {
        this.authService = authService;
        this.mfaService = mfaService;
        this.cookieService = cookieService;
        this.desktopHandoffService = desktopHandoffService;
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

        AuthDto.RegisterResponse resp = authService.register(req, userAgent(servReq), clientIp(servReq));
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

        AuthDto.LoginResponse resp = authService.issueLoginResponse(user, userAgent(servReq), clientIp(servReq), req.deviceId(), req.deviceLabel(), req.rememberMe());
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

        String bodyToken = (body != null && body.refreshToken() != null && !body.refreshToken().isBlank())
                ? body.refreshToken().trim()
                : null;
        String cookieToken = extractRefreshCookie(servReq);

        // Prioritize explicit body token if provided, fallback to HttpOnly cookie
        String rawToken = (bodyToken != null) ? bodyToken : cookieToken;
        if (rawToken == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No refresh token provided");

        AuthDto.AccessTokenResponse resp;
        try {
            resp = authService.refreshTokens(rawToken, userAgent(servReq), clientIp(servReq));
        } catch (ResponseStatusException e) {
            // If primary token failed (e.g. stale body or stale cookie) and the other is available, attempt fallback
            String fallbackToken = rawToken.equals(bodyToken) ? cookieToken : bodyToken;
            if (fallbackToken != null && !fallbackToken.equals(rawToken)) {
                try {
                    resp = authService.refreshTokens(fallbackToken, userAgent(servReq), clientIp(servReq));
                } catch (ResponseStatusException ignored) {
                    throw e; // rethrow the original exception if fallback also fails
                }
            } else {
                throw e;
            }
        }

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
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) AuthDto.RefreshTokenRequest body,
            HttpServletRequest servReq,
            HttpServletResponse servRes) {
        String bodyToken = (body != null && body.refreshToken() != null && !body.refreshToken().isBlank())
                ? body.refreshToken().trim()
                : null;
        String cookieToken = extractRefreshCookie(servReq);
        String rawToken = (bodyToken != null) ? bodyToken : cookieToken;

        if (rawToken != null) {
            try {
                authService.logout(rawToken);
            } catch (Exception ignored) {
                // Best-effort session revocation
            }
        }
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

    private String clientIp(HttpServletRequest request) {
        if (trustForwardedHeaders) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    // ── Desktop Handoff (RFC 8252 simplified) ────────────────────────────────────

    /**
     * Issues a one-time, 60-second handoff code for the currently authenticated browser user.
     * The code is navigated to /open-app?code=... which fires a deep-link into the desktop app.
     * Requires a valid Bearer JWT (the browser must already be logged in).
     */
    @PostMapping("/desktop-handoff/issue")
    public ResponseEntity<java.util.Map<String, String>> issueDesktopHandoffCode(
            @RequestBody(required = false) java.util.Map<String, String> body,
            Authentication auth) {
        UUID userId = currentUserId(auth);
        String deviceId    = body != null ? body.get("deviceId")    : null;
        String deviceLabel = body != null ? body.get("deviceLabel") : null;
        String code = desktopHandoffService.issueCode(userId, deviceId, deviceLabel);
        return ResponseEntity.ok(java.util.Map.of("code", code));
    }

    /**
     * Exchanges a one-time handoff code for a full token pair (access + refresh).
     * This endpoint is PUBLIC — no JWT required — because the desktop app calling it
     * does not yet have a token. The code itself is the credential.
     *
     * Returns tokens in the response BODY (not a cookie) so the desktop app can store them.
     */
    @PostMapping("/desktop-handoff/exchange")
    public ResponseEntity<java.util.Map<String, Object>> exchangeDesktopHandoffCode(
            @RequestBody java.util.Map<String, String> body,
            HttpServletRequest request) {
        String code = body.get("code");
        String userAgent = request.getHeader("User-Agent");
        TokenPair tokens = desktopHandoffService.exchangeCode(code, userAgent, clientIp(request));
        return ResponseEntity.ok(java.util.Map.of(
                "accessToken",     tokens.accessToken(),
                "refreshToken",    tokens.refreshToken(),
                "accessExpiresAt", tokens.accessExpiresAt().toString(),
                "refreshExpiresAt", tokens.refreshExpiresAt().toString()
        ));
    }
}
