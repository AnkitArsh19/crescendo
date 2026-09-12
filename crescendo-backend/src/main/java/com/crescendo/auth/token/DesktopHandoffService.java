package com.crescendo.auth.token;

import com.crescendo.security.AppUserDetails;
import com.crescendo.security.JWTService;
import com.crescendo.security.TokenPair;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and redeems one-time desktop handoff codes (RFC 8252 simplified pattern).
 *
 * <p>The browser calls {@link #issueCode} after verifying the user is logged in —
 * it receives a short-lived raw code. The code is navigated to /open-app?code=...,
 * which triggers crescendo://auth/callback?code=... deep-link. The desktop app then
 * calls {@link #exchangeCode} to swap the code for a real token pair.
 *
 * <p>Tokens NEVER appear in a URL; only the short-lived, single-use code does.
 */
@Service
public class DesktopHandoffService {

    private static final int CODE_TTL_SECONDS = 60;

    private final DesktopHandoffCodeRepository repository;
    private final User_commandRepository userRepository;
    private final JWTService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    public DesktopHandoffService(DesktopHandoffCodeRepository repository,
                                  User_commandRepository userRepository,
                                  JWTService jwtService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    /**
     * Issues a new handoff code tied to the authenticated user.
     *
     * @param userId      the authenticated user's ID
     * @param deviceId    optional device fingerprint from the desktop app
     * @param deviceLabel optional human-readable device name
     * @return the raw (unhashed) code to pass to the browser — stored only as a hash
     */
    @Transactional
    public String issueCode(UUID userId, String deviceId, String deviceLabel) {
        User_command user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        String raw = randomCode();
        String hash = hashCode(raw);
        Instant now = Instant.now();

        DesktopHandoffCode code = new DesktopHandoffCode(
                UUID.randomUUID(), user, hash,
                now.plusSeconds(CODE_TTL_SECONDS),
                deviceId, deviceLabel);
        repository.save(code);
        return raw;
    }

    /**
     * Redeems a handoff code and returns a full token pair for the desktop app.
     *
     * <p>Validates:
     * <ul>
     *   <li>Code exists (unknown = 401)</li>
     *   <li>Code has not been used before (replay = 401)</li>
     *   <li>Code has not expired (stale = 401)</li>
     * </ul>
     * The code is marked used atomically before tokens are issued.
     *
     * @param rawCode   the code received from the deep-link URL parameter
     * @param userAgent HTTP User-Agent from the desktop app (for session fingerprinting)
     * @param clientIp  client IP (for session fingerprinting and anomaly detection)
     * @return a fresh token pair (access + refresh); should be returned in response body, NOT a cookie
     */
    @Transactional
    public TokenPair exchangeCode(String rawCode, String userAgent, String clientIp) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Handoff code is required");
        }

        String hash = hashCode(rawCode);
        DesktopHandoffCode handoff = repository.findByCodeHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid handoff code"));

        if (handoff.getUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Handoff code already redeemed");
        }
        if (handoff.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Handoff code expired");
        }

        // Mark as consumed BEFORE issuing tokens — atomic-ish within the transaction.
        handoff.setUsedAt(Instant.now());
        repository.save(handoff);

        User_command user = handoff.getUser();
        AppUserDetails principal = AppUserDetails.from(user, Optional.empty());

        return jwtService.issueTokenPair(
                user, principal, userAgent,
                clientIp,
                handoff.getDeviceId(),
                handoff.getDeviceLabel(),
                false);
    }

    /**
     * Scheduled cleanup: removes expired codes every 5 minutes.
     * Codes expire in 60 seconds; this is purely a housekeeping measure.
     */
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void cleanupExpiredCodes() {
        repository.deleteAllExpiredBefore(Instant.now());
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private String randomCode() {
        byte[] buf = new byte[32]; // 256 bits of entropy
        secureRandom.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private String hashCode(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot hash handoff code", e);
        }
    }
}
