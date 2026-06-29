package com.crescendo.security;

import com.crescendo.auth.domain_event.SuspiciousSessionIpEvent;
import com.crescendo.auth.domain_event.UserSessionCreatedEvent;
import com.crescendo.shared.domain.event.DomainEventPublisher;
import com.crescendo.shared.domain.valueobject.IpAddress;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.user_session.UserSession;
import com.crescendo.user.user_command.user_session.UserSessionRepository;
import jakarta.transaction.Transactional;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

@Service
public class JWTService {

    private final UserSessionRepository userSessionRepository;
    private final DomainEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access.expiration}")
    private long accessExpirationMs;

    @Value("${jwt.refresh.expiration}")
    private long refreshExpirationMs;

    @Value("${jwt.refresh.rotate:true}")
    private boolean rotateOnRefresh;

    public JWTService(UserSessionRepository userSessionRepository, DomainEventPublisher eventPublisher) {
        this.userSessionRepository = userSessionRepository;
        this.eventPublisher = eventPublisher;
    }

    /// Issue a fresh access + refresh token pair for first-time login (no device fingerprint known).
    public TokenPair issueTokenPair(User_command user, AppUserDetails principal, String userAgent) {
        return issueTokenPair(user, principal, userAgent, false);
    }

    public TokenPair issueTokenPair(User_command user, AppUserDetails principal, String userAgent, boolean rememberMe) {
        Instant now = Instant.now();
        // 30 days for remember-me sessions, default otherwise
        Long ttl = rememberMe ? 2_592_000_000L : null;
        RefreshIssue refreshIssue = createRefreshSession(user, userAgent, null, null, null, null, now, ttl);
        String access = buildAccessToken(principal, refreshIssue.sessionId(), now);
        return new TokenPair(access, refreshIssue.plainToken(), now.plusMillis(accessExpirationMs), refreshIssue.expiresAt());
    }

    /// Overload that also records full device fingerprint (IP, device ID, human-readable label).
    /// Used when the client sends device metadata alongside the login request.
    public TokenPair issueTokenPair(User_command user, AppUserDetails principal, String userAgent,
                                    String clientIp, String deviceId, String deviceLabel, boolean rememberMe) {
        Instant now = Instant.now();
        Long ttl = rememberMe ? 2_592_000_000L : null;
        RefreshIssue refreshIssue = createRefreshSession(user, userAgent, clientIp, deviceId, deviceLabel, null, now, ttl);
        String access = buildAccessToken(principal, refreshIssue.sessionId(), now);
        return new TokenPair(access, refreshIssue.plainToken(), now.plusMillis(accessExpirationMs), refreshIssue.expiresAt());
    }

    /// Validates a presented refresh token and issues a new token pair.
    /// If rotateOnRefresh=true (default), the old session is revoked and a brand-new session is created.
    /// Also implements refresh token reuse detection — if a previously revoked (but not yet expired)
    /// token is presented again, ALL sessions for that user are revoked immediately (breach response).
    @Transactional
    public Optional<TokenPair> refresh(String presentedRefreshToken, String userAgent, String clientIp) {
        String hash = hashRefresh(presentedRefreshToken);
        Instant now = Instant.now();
        Optional<TokenPair> active = userSessionRepository.findActiveByHash(hash, now).map(session -> {
            session.setLastUsedAt(now);
            
            // Anomaly Detection: Compare original clientIp with new clientIp
            IpAddress newIp = IpAddress.of(clientIp);
            if (session.getClientIp() != null && session.getClientIp().isSuspiciouslyDifferentFrom(newIp)) {
                eventPublisher.publish(new SuspiciousSessionIpEvent(
                        session.getUser().getId(),
                        session.getId(),
                        session.getClientIp().value(),
                        newIp != null ? newIp.value() : "unknown"
                ));
            }
            session.setLastIp(newIp);
            
            User_command user = session.getUser();
            AppUserDetails principal = AppUserDetails.from(user, Optional.empty());

            String newAccess = buildAccessToken(principal, session.getId().toString(), now);
            Instant refreshExpires = session.getExpiresAt();
            String outRefresh = presentedRefreshToken;

            if (rotateOnRefresh) {
                session.setRevokedAt(now);
                RefreshIssue ri = createRefreshSession(user, userAgent, 
                        session.getLastIp() != null ? session.getLastIp().value() : null, 
                        session.getDeviceId() != null ? session.getDeviceId().value() : null, 
                        session.getDeviceLabel(), 
                        session.getClientIp() != null ? session.getClientIp().value() : null, 
                        now, null);
                outRefresh = ri.plainToken();
                refreshExpires = ri.expiresAt();
                newAccess = buildAccessToken(principal, ri.sessionId(), now);
            }
            return new TokenPair(newAccess, outRefresh, now.plusMillis(accessExpirationMs), refreshExpires);
        });
        if (active.isPresent()) return active;

        // Rotation reuse detection: a valid-but-revoked token being presented again is a strong
        // signal that a previously issued token was stolen. We nuke all active sessions
        // for that user as a precaution — they will need to log in again.
        userSessionRepository.findByRefreshTokenHash(hash).ifPresent(reused -> {
            if (reused.getRevokedAt() != null && reused.getExpiresAt().isAfter(now)) {
                revokeAllForUser(reused.getUser().getId());
            }
        });
        return Optional.empty();
    }

    @Transactional
    public boolean revokeRefresh(String refreshToken) {
        String hash = hashRefresh(refreshToken);
        Instant now = Instant.now();
        return userSessionRepository.findActiveByHash(hash, now).map(s -> {
            s.setRevokedAt(now);
            return true;
        }).orElse(false);
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        Instant now = Instant.now();
        var active = userSessionRepository.findAllActiveByUserId(userId, now);
        active.forEach(s -> s.setRevokedAt(now));
    }

    public boolean validateAccessToken(String token, UserDetails details) {
        try {
            String subject = extractUserName(token);
            return subject.equals(details.getUsername()) && !isExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUserName(String token) { return extractClaim(token, Claims::getSubject); }
    public Instant extractExpiryInstant(String token) { return extractClaim(token, c -> c.getExpiration().toInstant()); }

    /// Extracts the session ID (sid claim) from an access token.
    /// Returns null if the claim is absent (should not happen for tokens issued by us).
    public String extractSessionId(String token) { return extractClaim(token, c -> c.get("sid", String.class)); }

    public boolean isExpired(String token) { return extractExpiryInstant(token).isBefore(Instant.now()); }

    private <T> T extractClaim(String token, Function<Claims,T> resolver) {
        return resolver.apply(parseClaims(token));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token).getPayload();
    }

    /// Builds a signed JWT access token.
    /// Custom claims:
    ///   uid  = user's UUID (avoids a DB lookup to get user ID from subject/email)
    ///   role = user's role for quick authorization checks in filters
    ///   jti  = unique token ID (allows individual token revocation if needed)
    ///   sid  = session ID linking this access token to its parent refresh session
    private String buildAccessToken(AppUserDetails principal, String sessionId, Instant now) {
        Map<String,Object> claims = new HashMap<>();
        claims.put("uid", principal.getId().toString());
        claims.put("role", principal.getRole().name());
        claims.put("jti", UUID.randomUUID().toString());
        if (sessionId != null) {
            claims.put("sid", sessionId);
        }
        Instant exp = now.plusMillis(accessExpirationMs);
        return Jwts.builder()
                .claims(claims)
                .subject(principal.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey())
                .compact();
    }

    private RefreshIssue createRefreshSession(User_command user, String userAgent,
                                              String clientIp, String deviceId, String deviceLabel, String originalClientIp,
                                              Instant now, Long customTtlMs) {
        String raw = randomToken();
        String hash = hashRefresh(raw);
        Instant expires = now.plusMillis(customTtlMs != null ? customTtlMs : refreshExpirationMs);
        UUID sessionId = UUID.randomUUID();
        UserSession session = new UserSession(sessionId, user, hash, expires);
        session.applyFingerprint(userAgent, clientIp, deviceId, deviceLabel);
        if (originalClientIp != null) {
            session.setClientIp(IpAddress.of(originalClientIp));
        }
        userSessionRepository.save(session);

        // Publish a domain event so subscribers (security alerts, session limits, analytics)
        // can react to new session creation without coupling to JWTService directly.
        eventPublisher.publish(new UserSessionCreatedEvent(user.getId(), sessionId, clientIp, deviceLabel));

        return new RefreshIssue(sessionId.toString(), raw, expires);
    }

    /// Generates a cryptographically random 48-byte token, URL-safe Base64 encoded.
    /// 48 bytes = 384 bits of entropy — far beyond brute-force reach.
    private String randomToken() {
        byte[] buf = new byte[48];
        secureRandom.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    /// One-way SHA-256 hash of the raw refresh token for safe DB storage.
    /// Only the hash is persisted — the raw token lives only in the client's cookie.
    /// If the DB is compromised, hashes cannot be replayed against the auth endpoint.
    private String hashRefresh(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot hash refresh token", e);
        }
    }

    /// Derives the HMAC-SHA signing key from the configured secret.
    /// Accepts either a raw UTF-8 string or a Base64-encoded secret (auto-detected).
    /// Base64 encoding is preferred in production to allow arbitrary byte sequences.
    private SecretKey signingKey() {
        byte[] keyBytes;
        if (looksBase64(jwtSecret)) {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } else {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /// Attempts a Base64 decode — if it succeeds the secret is treated as Base64, else as plain UTF-8.
    private boolean looksBase64(String s) {
        try { Decoders.BASE64.decode(s); return true; } catch (IllegalArgumentException e) { return false; }
    }

    /// Internal result carrier for the refresh session creation helper —
    /// bundles the new session ID, the raw (unhashed) token, and its expiry together.
    private record RefreshIssue(String sessionId, String plainToken, Instant expiresAt) {}
}

