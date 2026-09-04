package com.crescendo.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
     */
    private void incrementDailyCount(UUID userId) {
        if (redisTemplate == null) return;

        String today = LocalDate.now(ZoneOffset.UTC).toString();
        Duration ttl = Duration.ofHours(36); // Keep past UTC midnight for rolling overlap
        try {
            if (userId != null) {
                String userKey = "crescendo:ratelimit:ai:rpd:user:" + userId + ":" + today;
                Long count = redisTemplate.opsForValue().increment(userKey);
                if (count != null && count == 1) {
                    redisTemplate.expire(userKey, ttl);
                }
            }

            String platformKey = "crescendo:ratelimit:ai:rpd:global:" + today;
            Long pCount = redisTemplate.opsForValue().increment(platformKey);
            if (pCount != null && pCount == 1) {
                redisTemplate.expire(platformKey, ttl);
            }
        } catch (Exception ex) {
            log.warn("Redis daily rate limit increment failed: {}", ex.getMessage());
        }
    }

    /**
     * Acquires an RPM slot for the request.
     * If limits are exceeded, holds the thread in a queue and polls until capacity frees up.
     */
    private void acquireRpmSlotWithQueue(UUID userId) {
        if (redisTemplate == null) return;

        long startTime = System.currentTimeMillis();
        boolean queued = false;

        while (true) {
            long currentMinute = Instant.now().getEpochSecond() / 60;
            String globalRpmKey = "crescendo:ratelimit:ai:rpm:global:" + currentMinute;
            String userRpmKey = userId != null ? "crescendo:ratelimit:ai:rpm:user:" + userId + ":" + currentMinute : null;

            try {
                // Read current values without incrementing
                String globalVal = redisTemplate.opsForValue().get(globalRpmKey);
                long currentGlobal = globalVal != null ? Long.parseLong(globalVal) : 0L;

                long currentUser = 0L;
                if (userRpmKey != null) {
                    String userVal = redisTemplate.opsForValue().get(userRpmKey);
                    currentUser = userVal != null ? Long.parseLong(userVal) : 0L;
                }

                boolean globalHasRoom = currentGlobal < platformRpmLimit;
                boolean userHasRoom = userId == null || currentUser < userRpmLimit;

                if (globalHasRoom && userHasRoom) {
                    // Claim slot
                    Long newGlobal = redisTemplate.opsForValue().increment(globalRpmKey);
                    if (newGlobal != null && newGlobal == 1) {
                        redisTemplate.expire(globalRpmKey, Duration.ofSeconds(75));
                    }

                    if (userRpmKey != null) {
                        Long newUser = redisTemplate.opsForValue().increment(userRpmKey);
                        if (newUser != null && newUser == 1) {
                            redisTemplate.expire(userRpmKey, Duration.ofSeconds(75));
                        }
                    }

                    if (queued) {
                        log.info("RPM slot acquired after {}ms in queue for user={}", System.currentTimeMillis() - startTime, userId);
                    }
                    return; // Successfully acquired slot!
                }

                // Capacity saturated -> Queue the request
                if (!queued) {
                    log.info("AI request for user={} queued due to RPM limit (global={}/{}, user={}/{}). Holding...",
                            userId, currentGlobal, platformRpmLimit, currentUser, userRpmLimit);
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

                // Poll wait between 1500ms and 2000ms
                Thread.sleep(1750);

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "AI request queued was interrupted.");
            } catch (ResponseStatusException rse) {
                throw rse;
            } catch (Exception ex) {
                log.warn("Redis RPM queue operation failed (failing open): {}", ex.getMessage());
                return; // Fail open on redis errors
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
