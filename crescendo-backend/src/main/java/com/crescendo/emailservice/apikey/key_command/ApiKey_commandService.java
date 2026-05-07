package com.crescendo.emailservice.apikey.key_command;

import com.crescendo.emailservice.apikey.ApiKeyDto;
import com.crescendo.emailservice.apikey.key_query.ApiKey_query;
import com.crescendo.emailservice.apikey.key_query.ApiKey_queryRepository;
import com.crescendo.emailservice.domain_event.ApiKeyCreatedEvent;
import com.crescendo.emailservice.domain_event.ApiKeyRevokedEvent;
import com.crescendo.shared.domain.event.DomainEventPublisher;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
 * Key format: re_ + 32 random bytes (Base64url) ≈ 46 chars total.
 * Storage: SHA-256 hash (high-entropy secret doesn't need BCrypt).
 * Prefix: first 8 chars for display and fast lookup.
 */
@Service
@Transactional
public class ApiKey_commandService {

    private static final int KEY_BYTES = 32;
    private static final String KEY_PREFIX = "re_";
    private static final int DISPLAY_PREFIX_LENGTH = 8;
    private static final int MAX_KEYS_PER_USER = 5;

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
        long activeCount = commandRepo.countByUserIdAndRevokedAtIsNull(userId);
        if (activeCount >= MAX_KEYS_PER_USER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Maximum API keys reached (" + MAX_KEYS_PER_USER + ")");
        }

        // Generate cryptographically random key
        byte[] randomBytes = new byte[KEY_BYTES];
        secureRandom.nextBytes(randomBytes);
        String plainKey = KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        String prefix = plainKey.substring(0, DISPLAY_PREFIX_LENGTH);
        String hashedKey = sha256Hex(plainKey);

        UUID apiKeyId = UUID.randomUUID();
        ApiKey_command apiKey = new ApiKey_command(apiKeyId, userId, req.name(), hashedKey, prefix);
        commandRepo.save(apiKey);

        // Sync to query database (no hash on read side)
        queryRepo.save(new ApiKey_query(apiKeyId, userId, req.name(), prefix));

        eventPublisher.publish(new ApiKeyCreatedEvent(apiKeyId, userId, prefix));

        return new ApiKeyDto.ApiKeyCreatedResponse(apiKeyId, req.name(), prefix, plainKey);
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
                .ifPresent(q -> q.setRevokedAt(apiKey.getRevokedAt()));

        eventPublisher.publish(new ApiKeyRevokedEvent(apiKeyId, userId));
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
}
