package com.crescendo.security.mfa;

import com.crescendo.security.AppUserDetails;
import com.crescendo.security.RefreshTokenCookieService;
import com.crescendo.security.TokenPair;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * All MFA endpoints live under /mfa.
 *
 * Enrollment flow (must be logged in):
 *   POST  /mfa/enroll/start          — generate secret + QR code
 *   POST  /mfa/enroll/confirm        — verify first code, enable MFA, receive backup codes
 *   PATCH /mfa/toggle                — enable or disable MFA after enrollment
 *   POST  /mfa/backup-codes/regenerate — get a fresh set of 10 backup codes
 *
 * Login flow (no JWT yet — called after POST /auth/login returns 202):
 *   POST  /mfa/challenge             — TOTP code login
 *   POST  /mfa/backup-code           — backup code login (when phone unavailable)
 */
@RestController
@RequestMapping("/mfa")
public class MFAController {

    private final MFAService mfaService;
    private final User_commandRepository userRepo;
    private final RefreshTokenCookieService cookieService;

    /// Controls whether the refresh-token cookie carries the Secure flag.
    @Value("${app.cookie.secure:false}")
    private boolean secureCookie;

    public MFAController(MFAService mfaService,
                         User_commandRepository userRepo,
                         RefreshTokenCookieService cookieService) {
        this.mfaService = mfaService;
        this.userRepo = userRepo;
        this.cookieService = cookieService;
    }

    // ENROLLMENT
    /**
     * POST /mfa/enroll/start
     * Generates a TOTP secret + a QR code for the authenticated user.
     * The user scans the QR in Google Authenticator or enters the secret manually.
     * MFA is NOT active yet — call /mfa/enroll/confirm to activate it.
     */
    @PostMapping("/enroll/start")
    public ResponseEntity<MFADto.MfaEnrollStartResponse> startEnrollment(Authentication auth) {
        UUID userId = currentUserId(auth);
        //auth.getPrincipal returns authenticated user object
        //safety check for not null object
        //Convert to AppUserDetails to use custom methods like getEmail
        String email = ((AppUserDetails) Objects.requireNonNull(auth.getPrincipal())).getEmail();
        MFADto.MfaEnrollStartResponse resp = mfaService.startEnrollment(userId, "Crescendo", email);
        return ResponseEntity.ok(resp);
    }

    /**
     * POST /mfa/enroll/confirm
     * User enters the 6-digit code shown by their authenticator app.
     * On success: MFA is marked verified + enabled, and 10 backup codes are returned.
     * Those backup codes are shown ONCE — save them.
     */
    @PostMapping("/enroll/confirm")
    public ResponseEntity<MFADto.MfaBackupCodesResponse> confirmEnrollment(
            @Valid @RequestBody MFADto.MfaEnrollConfirmRequest req,
            Authentication auth) {

        UUID userId = currentUserId(auth);
        MFADto.MfaBackupCodesResponse codes = mfaService.confirmEnrollment(userId, req.code())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "TOTP code is incorrect — check your authenticator app and try again"));

        return ResponseEntity.ok(codes);
    }

    /**
     * PATCH /mfa/toggle
     * Enables or disables MFA for the authenticated user.
     * MFA must have been confirmed at least once before it can be enabled.
     */
    @PatchMapping("/toggle")
    public ResponseEntity<Void> toggle(
            @Valid @RequestBody MFADto.MfaToggleRequest req,
            Authentication auth) {

        mfaService.toggle(currentUserId(auth), req.enabled());
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /mfa/backup-codes/regenerate
     * Deletes all existing backup codes and generates 10 new ones.
     * Use this when codes are compromised or running low.
     * The new codes are returned ONCE — save them.
     */
    @PostMapping("/backup-codes/regenerate")
    public ResponseEntity<MFADto.MfaBackupCodesResponse> regenerateBackupCodes(Authentication auth) {
        MFADto.MfaBackupCodesResponse resp = mfaService.regenerateBackupCodes(currentUserId(auth));
        return ResponseEntity.ok(resp);
    }

    // LOGIN CHALLENGE
    /**
     * POST /mfa/challenge
     * Called immediately after POST /auth/login returns 202 {mfaRequired: true}.
     * Verifies the 6-digit TOTP code and issues the full token pair on success.
     */
    @PostMapping("/challenge")
    public ResponseEntity<MFADto.MfaLoginChallengeResponse> challenge(
            @Valid @RequestBody MFADto.MfaLoginChallengeRequest req,
            HttpServletRequest servReq,
            HttpServletResponse servRes) {

        User_command user = userRepo.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        int code = Integer.parseInt(req.code());
        String clientIp = servReq.getHeader("X-Forwarded-For");
        if (clientIp == null) clientIp = servReq.getRemoteAddr();
        else clientIp = clientIp.split(",")[0].trim();
        Optional<TokenPair> tokenOpt = mfaService.completeLoginIfValid(user, code, userAgent(servReq), clientIp, req.deviceId(), req.deviceLabel());

        if (tokenOpt.isEmpty())
            return ResponseEntity.ok(new MFADto.MfaLoginChallengeResponse(false, null, null));

        TokenPair tokens = tokenOpt.get();
        setRefreshCookie(servRes, tokens.refreshToken(), tokens.refreshExpiresAt());
        return ResponseEntity.ok(new MFADto.MfaLoginChallengeResponse(true, tokens.accessToken(), tokens.refreshToken()));
    }

    /**
     * POST /mfa/backup-code
     * Alternative to /mfa/challenge for when the user has lost their authenticator device.
     * Backup codes are single-use (each code can only be consumed once).
     */
    @PostMapping("/backup-code")
    public ResponseEntity<MFADto.MfaUseBackupCodeResponse> useBackupCode(
            @Valid @RequestBody MFADto.MfaUseBackupCodeRequest req,
            HttpServletRequest servReq,
            HttpServletResponse servRes) {

        User_command user = userRepo.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        String backupClientIp = servReq.getHeader("X-Forwarded-For");
        if (backupClientIp == null) backupClientIp = servReq.getRemoteAddr();
        else backupClientIp = backupClientIp.split(",")[0].trim();
        Optional<TokenPair> tokenOpt = mfaService.useBackupCode(user, req.backupCode(), userAgent(servReq), backupClientIp, req.deviceId(), req.deviceLabel());

        if (tokenOpt.isEmpty())
            return ResponseEntity.ok(new MFADto.MfaUseBackupCodeResponse(false, null, null, 0));

        TokenPair tokens = tokenOpt.get();
        setRefreshCookie(servRes, tokens.refreshToken(), tokens.refreshExpiresAt());

        // Count remaining unused backup codes so the UI can warn the user if running low.
        int remaining = mfaService.countRemainingBackupCodes(user.getId());
        return ResponseEntity.ok(new MFADto.MfaUseBackupCodeResponse(true, tokens.accessToken(), tokens.refreshToken(), remaining));
    }

    private UUID currentUserId(Authentication auth) {
        return ((AppUserDetails) auth.getPrincipal()).getId();
    }

    private String userAgent(HttpServletRequest req) {
        return req.getHeader("User-Agent");
    }

    private void setRefreshCookie(HttpServletResponse res, String token, Instant expiresAt) {
        long ttlMs = Duration.between(Instant.now(), expiresAt).toMillis();
        cookieService.setRefreshToken(res, token, ttlMs, secureCookie);
    }
}
