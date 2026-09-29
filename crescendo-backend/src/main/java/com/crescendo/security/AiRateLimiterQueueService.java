package com.crescendo.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * AI Rate Limiter & Request Queue Service.
 *
 * <p>Enforces two-tiered rate limiting on shared Gemini / AI calls:
 * <ul>
 *   <li><b>Daily Quotas (RPD)</b>:
 *       Immediate rejection with 429 when exhausted.
 *       Per-user: 30 calls/day; Platform-wide: 480 calls/day (calibrated for Gemini 3.5 Flash Lite 500 RPD free tier with safety margin).</li>
 *   <li><b>Per-Minute Throttling (RPM) with Holding Queue</b>:
 *       When an RPM burst occurs (Per-user: >5/min or Platform-wide: >14/min),
 *       incoming requests are held in a waiting queue (polling every 1.5–2.0s
 *       up to a configurable timeout of 30s) until an RPM slot opens,
 *       rather than throwing an immediate 429 failure.</li>
 *   <li><b>Downstream 429 Backoff & Retry</b>:
 *       If Google returns HTTP 429 (RESOURCE_EXHAUSTED) during execution,
 *       the service backs off for 3 seconds and retries the action once.</li>
 * </ul>
 */
@Service
public class AiRateLimiterQueueService {

    private static final Logger log = LoggerFactory.getLogger(AiRateLimiterQueueService.class);

    // ── Atomic RPM slot claim ────────────────────────────────────────────────────
    //
    // KEYS[1] = global RPM key
    // KEYS[2] = user RPM key (empty string "" if userId is null)
    // ARGV[1] = platform RPM limit
    // ARGV[2] = user RPM limit
    // ARGV[3] = TTL in seconds for both keys
    //
    // Returns 1 if BOTH global AND user slots were successfully claimed, 0 otherwise.
    // Because all reads and writes happen inside a single EVAL call, no two concurrent
    // callers can both observe "room available" and both increment — the classic
    // check-then-act (GET … INCR) TOCTOU race is eliminated.
    private static final RedisScript<Long> CLAIM_RPM_SLOT_SCRIPT = RedisScript.of(
            "local gLimit  = tonumber(ARGV[1]) " +
            "local uLimit  = tonumber(ARGV[2]) " +
            "local ttlSecs = tonumber(ARGV[3]) " +
            "local gVal    = tonumber(redis.call('GET', KEYS[1]) or '0') " +
            "local uKey    = KEYS[2] " +
            "local uVal    = (uKey ~= '') and tonumber(redis.call('GET', uKey) or '0') or 0 " +
            "if gVal >= gLimit then return 0 end " +
            "if uKey ~= '' and uVal >= uLimit then return 0 end " +
            "local newG = redis.call('INCR', KEYS[1]) " +
            "if newG == 1 then redis.call('EXPIRE', KEYS[1], ttlSecs) end " +
            "if uKey ~= '' then " +
            "  local newU = redis.call('INCR', uKey) " +
            "  if newU == 1 then redis.call('EXPIRE', uKey, ttlSecs) end " +
            "end " +
            "return 1",
            Long.class
    );

    // ── Atomic INCR + EXPIRE (fixes the immortal-key bug in daily counters) ─────
    //
    // KEYS[1] = counter key
    // ARGV[1] = TTL in seconds
    // Returns the new counter value after increment.
    private static final RedisScript<Long> INCR_WITH_EXPIRE_SCRIPT = RedisScript.of(
            "local v = redis.call('INCR', KEYS[1]) " +
            "if v == 1 then redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1])) end " +
            "return v",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    private final int userRpmLimit;
    private final int platformRpmLimit;
    private final int userRpdLimit;
    private final int platformRpdLimit;
    private final long queueTimeoutMs;

    public AiRateLimiterQueueService(
            StringRedisTemplate redisTemplate,
            @Value("${crescendo.ai.ratelimit.user-rpm:5}") int userRpmLimit,
            @Value("${crescendo.ai.ratelimit.platform-rpm:14}") int platformRpmLimit,
            @Value("${crescendo.ai.ratelimit.user-rpd:30}") int userRpdLimit,
            @Value("${crescendo.ai.ratelimit.platform-rpd:480}") int platformRpdLimit,
            @Value("${crescendo.ai.ratelimit.queue-timeout-ms:30000}") long queueTimeoutMs) {
        this.redisTemplate = redisTemplate;
        this.userRpmLimit = userRpmLimit;
        this.platformRpmLimit = platformRpmLimit;
        this.userRpdLimit = userRpdLimit;
        this.platformRpdLimit = platformRpdLimit;
        this.queueTimeoutMs = queueTimeoutMs;
    }

    /**
     * Executes the supplied AI task within rate limits.
     *
     * <p>If daily limits are exceeded, immediately throws HTTP 429.
     * If RPM limits are saturated, pauses and queues the request until capacity opens up
     * or queue timeout is exceeded.
     *
     * @param userId caller's user ID (or null for anonymous/system)
     * @param action the AI operation to perform
     * @param <T>    return type of the AI operation
     * @return the result of the action
     */
    public <T> T executeWithRateLimiting(UUID userId, Supplier<T> action) {
        // 1. Daily Limit Check (Hard gate: Immediate denial, no queuing)
        checkAndEnforceDailyLimit(userId);

        // 2. RPM Slot Acquisition (Soft gate: Holding queue with backoff)
        acquireRpmSlotWithQueue(userId);

        // 3. Increment Daily Consumption
        incrementDailyCount(userId);

        // 4. Execution with transient 429 recovery
        try {
            return action.get();
        } catch (Exception ex) {
            if (isRateLimitException(ex)) {
                log.warn("Downstream AI provider returned rate limit (429). Backing off 3s and retrying once for user={}", userId);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "AI request interrupted during rate-limit backoff.");
                }
                return action.get();
            }
            throw ex;
        }
    }

    /**
     * Checks if caller or platform has exceeded daily request quota.
     * Denies immediately if limit reached.
     */
    private void checkAndEnforceDailyLimit(UUID userId) {
        if (redisTemplate == null) return;

        String today = LocalDate.now(ZoneOffset.UTC).toString();
        try {
            // User Daily Check
            if (userId != null) {
                String userKey = "crescendo:ratelimit:ai:rpd:user:" + userId + ":" + today;
                String userVal = redisTemplate.opsForValue().get(userKey);
                if (userVal != null && Long.parseLong(userVal) >= userRpdLimit) {
                    log.warn("User {} exceeded daily AI quota ({} >= {})", userId, userVal, userRpdLimit);
                    throw new ResponseStatusException(
                            HttpStatus.TOO_MANY_REQUESTS,
                            "Daily AI quota reached (" + userRpdLimit + " requests/day). Quota resets at 00:00 UTC. " +
                            "Connect your own Gemini API key in Settings for unlimited requests."
                    );
                }
            }

            // Platform Daily Check
            String platformKey = "crescendo:ratelimit:ai:rpd:global:" + today;
            String platformVal = redisTemplate.opsForValue().get(platformKey);
            if (platformVal != null && Long.parseLong(platformVal) >= platformRpdLimit) {
                log.warn("Platform global daily AI quota exceeded ({} >= {})", platformVal, platformRpdLimit);
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Platform daily AI capacity is currently exhausted. Please try again tomorrow or attach your own API key in Settings."
                );
            }
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception ex) {
            log.warn("Redis daily rate limit check failed (failing open): {}", ex.getMessage());
        }
    }

    /**
     * Increments the daily counter for user and platform upon successful dispatch.
     *
     * <p>Uses an atomic Lua {@code INCR + EXPIRE} script so the counter key can never
     * be left without a TTL (the old two-step INCR / EXPIRE approach could produce
     * a permanent "zombie" key if the JVM crashed between the two commands).
     */
    private void incrementDailyCount(UUID userId) {
        if (redisTemplate == null) return;

        String today = LocalDate.now(ZoneOffset.UTC).toString();
        long   ttlSeconds = Duration.ofHours(36).toSeconds(); // Keep past UTC midnight
        try {
            if (userId != null) {
                String userKey = "crescendo:ratelimit:ai:rpd:user:" + userId + ":" + today;
                redisTemplate.execute(INCR_WITH_EXPIRE_SCRIPT,
                        List.of(userKey), String.valueOf(ttlSeconds));
            }

            String platformKey = "crescendo:ratelimit:ai:rpd:global:" + today;
            redisTemplate.execute(INCR_WITH_EXPIRE_SCRIPT,
                    List.of(platformKey), String.valueOf(ttlSeconds));
        } catch (Exception ex) {
            log.warn("Redis daily rate limit increment failed: {}", ex.getMessage());
        }
    }

    /**
     * Acquires an RPM slot for the request using an atomic Lua script.
     *
     * <p>If capacity is available the script atomically increments both the global
     * and per-user counters in a single {@code EVAL} call, making it impossible for
     * two concurrent callers to both read "slot available" and both increment —
     * the TOCTOU race present in the previous GET / INCR implementation is gone.
     *
     * <p>If capacity is saturated the calling thread parks in a retry loop, polling
     * every ~1.75 s until a slot opens or the queue timeout is reached.
     */
    private void acquireRpmSlotWithQueue(UUID userId) {
        if (redisTemplate == null) return;

        long startTime = System.currentTimeMillis();
        boolean queued = false;

        while (true) {
            long   currentMinute = Instant.now().getEpochSecond() / 60;
            String globalRpmKey  = "crescendo:ratelimit:ai:rpm:global:" + currentMinute;
            String userRpmKey    = userId != null
                    ? "crescendo:ratelimit:ai:rpm:user:" + userId + ":" + currentMinute
                    : "";

            try {
                // Attempt to claim a slot atomically.
                // TTL = 75 s so keys auto-expire even if the minute boundary shifts.
                Long claimed = redisTemplate.execute(
                        CLAIM_RPM_SLOT_SCRIPT,
                        List.of(globalRpmKey, userRpmKey),
                        String.valueOf(platformRpmLimit),
                        String.valueOf(userRpmLimit),
                        "75"
                );

                if (Long.valueOf(1L).equals(claimed)) {
                    if (queued) {
                        log.info("RPM slot acquired after {}ms in queue for user={}",
                                System.currentTimeMillis() - startTime, userId);
                    }
                    return; // Slot successfully claimed — proceed to execution
                }

                // No slot available — enter / remain in the holding queue
                if (!queued) {
                    log.info("AI request for user={} queued (RPM capacity saturated). Holding...", userId);
                    queued = true;
                }

                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= queueTimeoutMs) {
                    log.warn("AI queue timeout ({}ms) reached for user={}", elapsed, userId);
                    throw new ResponseStatusException(
                            HttpStatus.TOO_MANY_REQUESTS,
                            "AI service is currently at capacity. Please try again in a few moments."
                    );
                }

                // Poll wait ~1.75 s before re-trying
                Thread.sleep(1750);

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "AI request queue was interrupted.");
            } catch (ResponseStatusException rse) {
                throw rse;
            } catch (Exception ex) {
                log.warn("Redis RPM claim failed (failing open): {}", ex.getMessage());
                return; // Fail open on Redis errors
            }
        }
    }

    /**
     * Identifies whether a downstream exception indicates rate limiting (HTTP 429).
     */
    private boolean isRateLimitException(Throwable throwable) {
        if (throwable == null) return false;
        if (throwable instanceof RestClientResponseException rce) {
            if (rce.getStatusCode().value() == 429) return true;
        }
        String msg = throwable.getMessage();
        if (msg != null && (msg.contains("429") || msg.contains("RESOURCE_EXHAUSTED") || msg.contains("Too Many Requests"))) {
            return true;
        }
        return isRateLimitException(throwable.getCause());
    }
}
