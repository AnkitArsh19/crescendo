package com.crescendo.security.webauthn;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * Issues and validates short-lived elevated-session tokens that gate sensitive
 * passkey management actions (add credential, delete credential).
 *
 * Design invariants:
 * <ul>
 *   <li>Each token is scoped to a specific action scope (e.g. {@code "passkey_mgmt"}) and
 *       a specific user ID. It cannot be reused for any other action or user.</li>
 *   <li>TTL is 10 minutes — generous enough for the user to complete the WebAuthn ceremony
 *       but short enough to limit the hijack window.</li>
 *   <li>The token is stored in Redis keyed by the opaque token value itself. Consuming
 *       the token (via {@link #consume}) deletes it so it cannot be replayed.</li>
 *   <li>This is NOT a general-purpose step-up mechanism. Adding new sensitive actions
 *       that should require step-up must explicitly call {@link #issue} with a distinct
 *       scope string, and the caller is responsible for verifying the scope matches.</li>
 * </ul>
 */
@Service
public class ElevatedSessionService {

    /** Scope constant for passkey add/delete operations. */
    public static final String SCOPE_PASSKEY_MGMT = "passkey_mgmt";

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String KEY_PREFIX = "elev:";

    private final StringRedisTemplate redis;
    private final SecureRandom secureRandom = new SecureRandom();

    public ElevatedSessionService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Issues a new elevated-session token for the given user and scope.
     *
     * @param userId authenticated user
     * @param scope  action scope (e.g. {@link #SCOPE_PASSKEY_MGMT})
     * @return the opaque token to be returned to the frontend
     */
    public String issue(UUID userId, String scope) {
        String token = randomToken();
        // value = "<userId>|<scope>" — both validated on consume
        redis.opsForValue().set(KEY_PREFIX + token, userId.toString() + "|" + scope, TTL);
        return token;
    }

    /**
     * Validates an elevated-session token and consumes it (single-use).
     *
     * @param token  the opaque token received from the frontend
     * @param userId the authenticated user's ID (must match the issued token)
     * @param scope  the required action scope (must match the issued scope)
     * @return true if the token is valid, belongs to this user, and covers this scope;
     *         false otherwise (expired, tampered, or wrong scope/user)
     */
    public boolean consume(String token, UUID userId, String scope) {
        if (token == null || token.isBlank()) return false;
        String key = KEY_PREFIX + token;
        String value = redis.opsForValue().get(key);
        if (value == null) return false;
        String[] parts = value.split("\\|", 2);
        if (parts.length != 2) return false;
        boolean valid = parts[0].equals(userId.toString()) && parts[1].equals(scope);
        if (valid) {
            redis.delete(key); // single-use: delete immediately after consumption
        }
        return valid;
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
