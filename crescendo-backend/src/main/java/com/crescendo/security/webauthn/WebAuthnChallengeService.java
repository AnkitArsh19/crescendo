package com.crescendo.security.webauthn;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Service
public class WebAuthnChallengeService {
    
    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();
    private static final String CHALLENGE_PREFIX = "webauthn:challenge:";
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);

    public WebAuthnChallengeService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generates a ceremony-specific, one-time challenge. The transaction ID is
     * unrelated to an email address or browser session, so concurrent ceremonies
     * cannot overwrite one another's challenge.
     */
    public ChallengeTransaction createChallenge(UUID expectedUserId) {
        byte[] challengeBytes = new byte[32];
        secureRandom.nextBytes(challengeBytes);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);
        byte[] transactionBytes = new byte[32];
        secureRandom.nextBytes(transactionBytes);
        String transactionId = Base64.getUrlEncoder().withoutPadding().encodeToString(transactionBytes);

        // Base64URL challenge contains neither ':' nor a UUID, making this compact
        // representation unambiguous without introducing a JSON serializer.
        String value = challenge + ":" + (expectedUserId == null ? "" : expectedUserId);
        redisTemplate.opsForValue().set(CHALLENGE_PREFIX + transactionId, value, CHALLENGE_TTL);
        return new ChallengeTransaction(transactionId, challenge);
    }

    /**
     * Retrieves and deletes the challenge for the given session ID.
     * WebAuthn challenges must be one-time use, so fetching it removes it from Redis.
     */
    public ChallengeContext consumeChallenge(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            return null;
        }
        String key = CHALLENGE_PREFIX + transactionId;
        String value = redisTemplate.opsForValue().getAndDelete(key);
        if (value == null) {
            return null;
        }

        int delimiter = value.lastIndexOf(':');
        String challenge = value.substring(0, delimiter);
        String userId = value.substring(delimiter + 1);
        return new ChallengeContext(challenge, userId.isEmpty() ? null : UUID.fromString(userId));
    }

    public record ChallengeTransaction(String transactionId, String challenge) {}

    public record ChallengeContext(String challenge, UUID expectedUserId) {}
}
