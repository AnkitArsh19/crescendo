package com.crescendo.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Generic sliding-window rate limiter backed by Redis INCR + EXPIRE.
 *
 * <p>Provides a 2-layered approach to rate limiting (inspired by n8n's production
 * rate-limit service):
 * <ul>
 *   <li>Layer 1: IP-based — blocks volumetric bot attacks before they hit auth logic.</li>
 *   <li>Layer 2: Keyed (email/username) — blocks targeted dictionary attacks per identity.</li>
 * </ul>
 *
 * <p>All callers share this single service to avoid duplicating the Redis sliding-window
 * logic that was previously embedded in {@link ApiKeyAuthenticationFilter}.
 */
@Service
public class RateLimitingService {

    private static final String KEY_SEPARATOR = ":";

    private final StringRedisTemplate redisTemplate;

    public RateLimitingService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks whether the given identifier has exceeded the rate limit.
     *
     * <p>Uses Redis INCR + EXPIRE for O(1) sliding-window enforcement.
     * On the first request in a window the TTL is set; subsequent requests
     * are counted until the key expires naturally.
     *
     * @param namespace   logical name for this rate-limit (e.g. "auth:ip", "auth:email")
     * @param identifier  the value being throttled (e.g. the client IP, the email address)
     * @param maxRequests maximum allowed requests in the window (inclusive)
     * @param window      the time window for the counter
     * @return {@code true} if the caller has EXCEEDED the limit and should be rejected
     */
    public boolean isRateLimited(String namespace, String identifier, int maxRequests, Duration window) {
        String key = "crescendo:ratelimit:" + namespace + KEY_SEPARATOR + sanitize(identifier);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, window);
        }
        return count != null && count > Math.max(1, maxRequests);
    }

    /**
     * Returns the current request count for the given namespace+identifier without
     * incrementing it. Useful for building 429 response headers.
     *
     * @return the current count, or 0 if no key exists
     */
    public long currentCount(String namespace, String identifier) {
        String key = "crescendo:ratelimit:" + namespace + KEY_SEPARATOR + sanitize(identifier);
        String val = redisTemplate.opsForValue().get(key);
        return val == null ? 0L : Long.parseLong(val);
    }

    /**
     * Sanitizes an identifier to prevent Redis key injection.
     * Colons are stripped since they are used as namespace separators.
     */
    private String sanitize(String identifier) {
        if (identifier == null) return "unknown";
        return identifier.replace(":", "_").trim();
    }
}
