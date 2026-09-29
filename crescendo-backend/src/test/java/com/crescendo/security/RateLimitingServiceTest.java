package com.crescendo.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RateLimitingService} (Token Bucket implementation).
 *
 * <p>The Lua script itself is tested by the integration test suite against a real
 * Redis instance (Testcontainers).  These unit tests mock the Redis execute call
 * to verify the Java-side logic:
 * <ul>
 *   <li>Correctly maps the [allowed=1, remaining] Lua reply to an allowed result.</li>
 *   <li>Correctly maps the [allowed=0, waitMs] Lua reply to a rejected result with
 *       the right {@code retryAfterMs} value.</li>
 *   <li>Fails open (allows) when Redis throws.</li>
 *   <li>Sanitizes colons in identifiers before building the Redis key.</li>
 *   <li>The convenience {@code isRateLimited} wrapper returns the expected boolean.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class RateLimitingServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingService(redisTemplate);
    }

    // ── tryConsume: allowed path ─────────────────────────────────────────────────

    @Test
    void tryConsume_whenLuaReturnsAllowed_returnsAllowedResultWithRemainingTokens() {
        // Lua returns [1 (allowed), 4 (remaining tokens)]
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(1L, 4L));

        RateLimitingService.TokenBucketResult result =
                rateLimitingService.tryConsume("auth:ip", "192.168.1.1", 5, 5);

        assertTrue(result.allowed());
        assertEquals(4L, result.remaining());
        assertEquals(0L, result.retryAfterMs());
        assertEquals(5,  result.limit());
    }

    @Test
    void tryConsume_firstRequestInWindow_isAllowed() {
        // Lua returns [1 (allowed), 4 remaining] — bucket had 5, consumed 1
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(1L, 4L));

        RateLimitingService.TokenBucketResult result =
                rateLimitingService.tryConsume("auth:ip", "192.168.1.1", 5, 5);

        assertTrue(result.allowed());
    }

    // ── tryConsume: rejected path ────────────────────────────────────────────────

    @Test
    void tryConsume_whenLuaReturnsRejected_returnsRejectedResultWithRetryAfterMs() {
        // Lua returns [0 (rejected), 6000 (wait 6 000 ms)]
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(0L, 6000L));

        RateLimitingService.TokenBucketResult result =
                rateLimitingService.tryConsume("auth:ip", "192.168.1.1", 5, 5);

        assertFalse(result.allowed());
        assertEquals(0L,    result.remaining());
        assertEquals(6000L, result.retryAfterMs());
        assertEquals(6L,    result.retryAfterSeconds()); // ceil(6000 / 1000)
        assertEquals(5,     result.limit());
    }

    @Test
    void tryConsume_retryAfterSeconds_roundsUp() {
        // 1 500 ms wait → ceil(1.5) = 2 seconds
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(0L, 1500L));

        RateLimitingService.TokenBucketResult result =
                rateLimitingService.tryConsume("auth:ip", "10.0.0.1", 5, 5);

        assertFalse(result.allowed());
        assertEquals(2L, result.retryAfterSeconds());
    }

    // ── fail-open behaviour ──────────────────────────────────────────────────────

    @Test
    void tryConsume_whenRedisThrows_failsOpenAndAllowsRequest() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new RuntimeException("Redis connection refused"));

        RateLimitingService.TokenBucketResult result =
                rateLimitingService.tryConsume("auth:ip", "192.168.1.1", 5, 5);

        assertTrue(result.allowed(), "Must fail open when Redis is unavailable");
    }

    @Test
    void tryConsume_whenLuaReturnsNull_failsOpenAndAllowsRequest() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(null);

        RateLimitingService.TokenBucketResult result =
                rateLimitingService.tryConsume("auth:ip", "192.168.1.1", 5, 5);

        assertTrue(result.allowed(), "Must fail open when Lua returns null");
    }

    // ── convenience isRateLimited wrapper ───────────────────────────────────────

    @Test
    void isRateLimited_whenAllowed_returnsFalse() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(1L, 3L));

        assertFalse(rateLimitingService.isRateLimited("auth:ip", "192.168.1.1", 5));
    }

    @Test
    void isRateLimited_whenRejected_returnsTrue() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(0L, 3000L));

        assertTrue(rateLimitingService.isRateLimited("auth:ip", "192.168.1.1", 5));
    }

    // ── key sanitization ─────────────────────────────────────────────────────────

    @Test
    void tryConsume_sanitizesColonsInIdentifier() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenAnswer(invocation -> {
                    // Verify the key passed to the script has no raw colons in the identifier part
                    @SuppressWarnings("unchecked")
                    List<String> keys = invocation.getArgument(1);
                    String key = keys.get(0);
                    // The identifier portion after "crescendo:ratelimit:auth:email:" must not
                    // contain a colon (colon in email is sanitized to underscore).
                    String identifierPart = key.substring("crescendo:ratelimit:auth:email:".length());
                    assertFalse(identifierPart.contains(":"),
                            "Colon in email identifier must be sanitized to underscore");
                    return List.of(1L, 4L);
                });

        rateLimitingService.tryConsume("auth:email", "user:name@example.com", 10, 10);
    }

    // ── zero / negative limit bypass ─────────────────────────────────────────────

    @Test
    void tryConsume_withZeroCapacity_alwaysAllowsWithoutCallingRedis() {
        RateLimitingService.TokenBucketResult result =
                rateLimitingService.tryConsume("auth:ip", "1.2.3.4", 0, 0);

        assertTrue(result.allowed());
        verifyNoInteractions(redisTemplate);
    }
}
