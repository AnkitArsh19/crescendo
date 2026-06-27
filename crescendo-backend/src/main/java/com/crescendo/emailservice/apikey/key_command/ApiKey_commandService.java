package com.crescendo.emailservice.apikey.key_command;

import com.crescendo.emailservice.apikey.ApiKeyDto;
import com.crescendo.emailservice.apikey.key_query.ApiKey_query;
import com.crescendo.emailservice.apikey.key_query.ApiKey_queryRepository;
import com.crescendo.emailservice.domain_event.ApiKeyCreatedEvent;
import com.crescendo.emailservice.domain_event.ApiKeyRevokedEvent;
import com.crescendo.publicapi.PublicApiScopes;
import com.crescendo.shared.domain.event.DomainEventPublisher;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Write-side service for API key management.
 *
 * Every mutation:
 *   1. Validates limits/ownership
 *   2. Writes to the command database
 *   3. Synchronously projects to the query database
 *   4. Publishes a domain event
 *
 * Key format: re_live_ + 32 random bytes (Base64url).
 * Storage: SHA-256 hash (high-entropy secret doesn't need BCrypt).
 * Prefix: first 8 chars for display and fast lookup.
 */
@Service
@Transactional
public class ApiKey_commandService {

    private static final int KEY_BYTES = 32;
    private static final String KEY_PREFIX = "re_live_";
    private static final int DISPLAY_PREFIX_LENGTH = 16;
    private static final int MAX_KEYS_PER_USER = 5;
    private static final int DEFAULT_EXPIRY_DAYS = 90;

    private final ApiKey_commandRepository commandRepo;
    private final ApiKey_queryRepository queryRepo;
    private final DomainEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKey_commandService(ApiKey_commandRepository commandRepo,
                                 ApiKey_queryRepository queryRepo,
                                 DomainEventPublisher eventPublisher) {
        this.commandRepo = commandRepo;
        this.queryRepo = queryRepo;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new API key for the user. Returns the plain key exactly once —
     * it is never stored or retrievable after this call.
     */
    public ApiKeyDto.ApiKeyCreatedResponse createApiKey(UUID userId, ApiKeyDto.CreateApiKeyRequest req) {
        Instant now = Instant.now();
        long activeCount = commandRepo.findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .filter(key -> key.isUsableAt(now))
                .count();
        if (activeCount >= MAX_KEYS_PER_USER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Maximum API keys reached (" + MAX_KEYS_PER_USER + ")");
        }

        String scopes = PublicApiScopes.serialize(req.scopes());
        int rateLimit = req.rateLimitPerMinute() == null
                ? 100
                : Math.max(1, Math.min(req.rateLimitPerMinute(), 10_000));
        int expiryDays = req.expiresInDays() == null ? DEFAULT_EXPIRY_DAYS : req.expiresInDays();

        return issueKey(userId, req.name(), scopes, rateLimit, now.plus(Duration.ofDays(expiryDays)));
    }

    /**
     * Replaces a key while optionally leaving the previous key usable for a short deployment window.
     * A zero-hour grace period behaves like immediate replacement.
     */
    public ApiKeyDto.ApiKeyCreatedResponse rotateApiKey(
            UUID userId, UUID apiKeyId, ApiKeyDto.RotateApiKeyRequest req) {
        ApiKey_command current = commandRepo.findByIdAndUserId(apiKeyId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found"));
        Instant now = Instant.now();
        if (!current.isUsableAt(now) || current.getReplacedByKeyId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only an active API key can be rotated");
        }

        int graceHours = req == null || req.gracePeriodHours() == null ? 24 : req.gracePeriodHours();
        ApiKeyDto.ApiKeyCreatedResponse replacement = issueKey(
                userId,
                current.getName(),
                current.getScopes(),
                current.getRateLimitPerMinute(),
                now.plus(Duration.ofDays(DEFAULT_EXPIRY_DAYS))
        );

        current.setReplacedByKeyId(replacement.id());
        current.setRotationGraceEndsAt(now.plus(Duration.ofHours(graceHours)));
        commandRepo.save(current);
        queryRepo.findByIdAndUserId(apiKeyId, userId).ifPresent(query -> {
            query.setReplacedByKeyId(replacement.id());
            query.setRotationGraceEndsAt(current.getRotationGraceEndsAt());
            queryRepo.save(query);
        });
        return replacement;
    }

    /**
     * Revokes an API key — marks it as revoked so it can no longer authenticate requests.
     */
    public void revokeApiKey(UUID userId, UUID apiKeyId) {
        ApiKey_command apiKey = commandRepo.findByIdAndUserId(apiKeyId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found"));

        if (apiKey.getRevokedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "API key already revoked");
        }

        apiKey.setRevokedAt(Instant.now());

        // Mark revoked on query side
        queryRepo.findByIdAndUserId(apiKeyId, userId)
                .ifPresent(q -> {
                    q.setRevokedAt(apiKey.getRevokedAt());
                    queryRepo.save(q);
                });

        eventPublisher.publish(new ApiKeyRevokedEvent(apiKeyId, userId));
    }

    private ApiKeyDto.ApiKeyCreatedResponse issueKey(
            UUID userId,
            String name,
            String scopes,
            int rateLimit,
            Instant expiresAt) {
        String plainKey;
        String prefix;
        do {
            byte[] randomBytes = new byte[KEY_BYTES];
            secureRandom.nextBytes(randomBytes);
            plainKey = KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
            prefix = plainKey.substring(0, DISPLAY_PREFIX_LENGTH);
        } while (commandRepo.existsByPrefix(prefix));

        UUID apiKeyId = UUID.randomUUID();
        ApiKey_command apiKey = new ApiKey_command(
                apiKeyId,
                userId,
                name,
                sha256Hex(plainKey),
                prefix,
                scopes,
                rateLimit,
                expiresAt
        );
        commandRepo.save(apiKey);
        queryRepo.save(new ApiKey_query(apiKeyId, userId, name, prefix, scopes, rateLimit, expiresAt));
        eventPublisher.publish(new ApiKeyCreatedEvent(apiKeyId, userId, prefix));

        return new ApiKeyDto.ApiKeyCreatedResponse(
                apiKeyId,
                name,
                prefix,
                plainKey,
                PublicApiScopes.parse(scopes),
                rateLimit,
                expiresAt
        );
    }

    /// SHA-256 hash for high-entropy API keys.
    /// Package-visible so the authentication filter can verify keys without BCrypt overhead.
    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static boolean securelyMatches(String rawKey, String expectedHash) {
        return MessageDigest.isEqual(
                sha256Hex(rawKey).getBytes(StandardCharsets.US_ASCII),
                expectedHash.getBytes(StandardCharsets.US_ASCII)
        );
    }
}
