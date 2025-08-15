package com.crescendo.security;

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
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access.expiration}")
    private long accessExpirationMs;

    @Value("${jwt.refresh.expiration}")
    private long refreshExpirationMs;

    @Value("${jwt.refresh.rotate:true}")
    private boolean rotateOnRefresh;

    public JWTService(UserSessionRepository userSessionRepository) {
        this.userSessionRepository = userSessionRepository;
    }

    public TokenPair issueTokenPair(User_command user, AppUserDetails principal, String userAgent) {
        Instant now = Instant.now();
        String access = buildAccessToken(principal, now);
        RefreshIssue refreshIssue = createRefreshSession(user, userAgent, now);
        return new TokenPair(access, refreshIssue.plainToken(), now.plusMillis(accessExpirationMs), refreshIssue.expiresAt());
    }

    @Transactional
    public Optional<TokenPair> refresh(String presentedRefreshToken, String userAgent) {
        String hash = hashRefresh(presentedRefreshToken);
        Instant now = Instant.now();
        Optional<TokenPair> active = userSessionRepository.findActiveByHash(hash, now).map(session -> {
            session.setLastUsedAt(now);
            User_command user = session.getUser();
            AppUserDetails principal = AppUserDetails.from(user, Optional.empty());

            String newAccess = buildAccessToken(principal, now);
            Instant refreshExpires = session.getExpiresAt();
            String outRefresh = presentedRefreshToken;

            if (rotateOnRefresh) {
                session.setRevokedAt(now);
                RefreshIssue ri = createRefreshSession(user, userAgent, now);
                outRefresh = ri.plainToken();
                refreshExpires = ri.expiresAt();
            }
            return new TokenPair(newAccess, outRefresh, now.plusMillis(accessExpirationMs), refreshExpires);
        });
        if (active.isPresent()) return active;

        // Rotation reuse detection: if token already revoked but still within original lifetime => possible replay
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

    public boolean isExpired(String token) { return extractExpiryInstant(token).isBefore(Instant.now()); }

    private <T> T extractClaim(String token, Function<Claims,T> resolver) {
        return resolver.apply(parseClaims(token));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token).getPayload();
    }

    private String buildAccessToken(AppUserDetails principal, Instant now) {
        Map<String,Object> claims = new HashMap<>();
        claims.put("uid", principal.getId().toString());
        claims.put("role", principal.getRole().name());
        claims.put("jti", UUID.randomUUID().toString());
        Instant exp = now.plusMillis(accessExpirationMs);
        return Jwts.builder()
                .claims(claims)
                .subject(principal.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey())
                .compact();
    }

    private RefreshIssue createRefreshSession(User_command user, String userAgent, Instant now) {
        String raw = randomToken();
        String hash = hashRefresh(raw);
        Instant expires = now.plusMillis(refreshExpirationMs);
        UserSession session = new UserSession(UUID.randomUUID(), user, hash, expires);
        session.setUserAgent(userAgent);
        userSessionRepository.save(session);
        return new RefreshIssue(raw, expires);
    }

    private String randomToken() {
        byte[] buf = new byte[48];
        secureRandom.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private String hashRefresh(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot hash refresh token", e);
        }
    }

    private SecretKey signingKey() {
        byte[] keyBytes;
        if (looksBase64(jwtSecret)) {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } else {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private boolean looksBase64(String s) {
        try { Decoders.BASE64.decode(s); return true; } catch (IllegalArgumentException e) { return false; }
    }

    private record RefreshIssue(String plainToken, Instant expiresAt) {}
}

