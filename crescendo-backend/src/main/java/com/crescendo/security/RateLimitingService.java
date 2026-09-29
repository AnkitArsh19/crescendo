package com.crescendo.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Redis-backed Token Bucket rate limiter.
 *
 * <p>Replaces the previous fixed-window (INCR + EXPIRE) implementation.
 * The core algorithm is a <b>Lazy-Refill Token Bucket</b> executed inside a single
 * atomic Lua script, eliminating two critical bugs in the old design:
 * <ol>
 *   <li><b>Immortal Key</b> — the old code called {@code INCR} and then {@code EXPIRE}
 *       as two separate commands.  A JVM crash or network blip between the two left
 *       the Redis key with {@code TTL = -1} (no expiry), permanently blocking the
 *       caller until a human ran {@code DEL} in Redis CLI.</li>
 *   <li><b>2x Window-Edge Burst</b> — fixed-window counters reset at a hard boundary,
 *       allowing a caller to fire {@code maxRequests} at second :59 and another
 *       {@code maxRequests} at second :01 of the next window (doubling the nominal
 *       limit in under two seconds).  A token bucket refills gradually, so bursts are
 *       bounded by the true capacity at all times.</li>
 * </ol>
 *
 * <h3>Algorithm — Lazy-Refill Token Bucket</h3>
 * Two values are persisted per identifier in Redis:
 * <ul>
 *   <li>{@code tokens}  — floating-point count of available tokens (stored as a
 *       millitokens integer to avoid floats in Redis strings).</li>
 *   <li>{@code lastRefillMs} — wall-clock epoch millisecond of the last refill.</li>
 * </ul>
 * On every request the Lua script:
 * <ol>
 *   <li>Reads both values (or initialises them if missing).</li>
 *   <li>Calculates elapsed time since the last refill.</li>
 *   <li>Adds {@code elapsed × refillRatePerMs} tokens, capped at {@code capacity}.</li>
 *   <li>If {@code tokens ≥ 1}, deducts 1 and returns {@code allowed=1}.</li>
 *   <li>Otherwise, calculates the exact milliseconds until the next token arrives
 *       and returns {@code allowed=0, retryAfterMs=<value>}.</li>
 *   <li>Saves the updated state and sets a TTL equal to the time needed to fully
 *       refill the bucket, so idle entries self-evict.</li>
 * </ol>
 * All six steps occur inside a single Redis {@code EVAL} call — no race condition
 * is possible because Redis executes Lua scripts atomically in its single-threaded
 * event loop.
 *
 * <h3>Two-layered auth rate limiting</h3>
 * <ul>
 *   <li>Layer 1 — IP-based: blocks volumetric / bot attacks.</li>
 *   <li>Layer 2 — Identity-keyed (email): blocks targeted dictionary attacks.</li>
 * </ul>
 *
 * <p>Fails open gracefully if Redis is unreachable, preventing infrastructure
 * transients from locking out legitimate users.
 */
@Service
public class RateLimitingService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingService.class);
    private static final String KEY_SEPARATOR = ":";

    // ── Lua Token Bucket Script ──────────────────────────────────────────────────
    //
    // KEYS[1] = bucket key   (e.g. "crescendo:ratelimit:auth:ip:1.2.3.4")
    // ARGV[1] = capacity          (max tokens, integer)
    // ARGV[2] = refillRatePerMs   (tokens refilled per millisecond × 1000, integer millitokens/ms)
    // ARGV[3] = nowMs             (current epoch millis as string)
    // ARGV[4] = ttlMs             (key TTL in millis — time to fully refill from 0 to capacity)
    //
    // Returns a two-element array:
    //   [0] = 1 (allowed) or 0 (rejected)
    //   [1] = remaining tokens after this request  (if allowed)
    //      OR milliseconds until next token arrives (if rejected)
    //
    // Stored values:
    //   KEYS[1]:tokens        — millitokens (capacity × 1000 unit)
    //   KEYS[1]:last          — epoch millis of last refill
    //
    // Using millitokens (×1000) so all arithmetic stays in integer space; Redis
    // does not have a native float type, and storing floats as strings introduces
    // rounding surprises.
    private static final RedisScript<List<Long>> TOKEN_BUCKET_SCRIPT = RedisScript.of(
            // ── initialise or read persisted state ──────────────────────────────
            "local tokensKey = KEYS[1] .. ':t' " +
            "local lastKey   = KEYS[1] .. ':l' " +
            "local capacity  = tonumber(ARGV[1]) * 1000 " +   // store as millitokens
            "local refillRpm = tonumber(ARGV[2]) " +           // tokens per minute
            "local nowMs     = tonumber(ARGV[3]) " +
            "local ttlMs     = tonumber(ARGV[4]) " +
            // ── read current state ───────────────────────────────────────────────
            "local rawTokens = redis.call('GET', tokensKey) " +
            "local rawLast   = redis.call('GET', lastKey) " +
            "local milliTokens = rawTokens and tonumber(rawTokens) or capacity " +
            "local lastMs      = rawLast   and tonumber(rawLast)   or nowMs " +
            // ── lazy refill ──────────────────────────────────────────────────────
            "local elapsedMs = math.max(0, nowMs - lastMs) " +
            // refillRpm tokens per 60 000 ms → refill = elapsed * rpm / 60000 * 1000 milliTokens
            "local refilled  = math.floor(elapsedMs * refillRpm * 1000 / 60000) " +
            "milliTokens = math.min(capacity, milliTokens + refilled) " +
            // ── attempt to consume one token (= 1000 millitokens) ────────────────
            "if milliTokens >= 1000 then " +
            "  milliTokens = milliTokens - 1000 " +
            "  local remainingTokens = math.floor(milliTokens / 1000) " +
            "  redis.call('SET', tokensKey, tostring(milliTokens), 'PX', tostring(ttlMs)) " +
            "  redis.call('SET', lastKey,   tostring(nowMs),        'PX', tostring(ttlMs)) " +
            "  return {1, remainingTokens} " +   // allowed, remaining tokens
            "else " +
            // ── calculate exact wait until next token ────────────────────────────
            "  local deficit = 1000 - milliTokens " +
            "  local waitMs  = math.ceil(deficit * 60000 / (refillRpm * 1000)) " +
            "  redis.call('SET', tokensKey, tostring(milliTokens), 'PX', tostring(ttlMs)) " +
            "  redis.call('SET', lastKey,   tostring(nowMs),        'PX', tostring(ttlMs)) " +
            "  return {0, waitMs} " +            // rejected, milliseconds to wait
            "end",
            // ── Java return type ─────────────────────────────────────────────────
            // RedisScript generic must be a single return type; we use List<Long>
            // which Spring Data Redis maps from a Lua multi-bulk reply.
            (Class<List<Long>>) (Class<?>) List.class
    );

    private final StringRedisTemplate redisTemplate;

    public RateLimitingService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ── Public API ───────────────────────────────────────────────────────────────

    /**
     * Result of a single token-bucket check.
     *
     * @param allowed        whether this request is allowed
     * @param remaining      if allowed: tokens remaining in the bucket after consumption;
     *                       if rejected: always 0
     * @param retryAfterMs   if rejected: exact milliseconds until the bucket refills
     *                       enough to allow one more request; if allowed: 0
     * @param limit          the configured bucket capacity (for header generation)
     */
    public record TokenBucketResult(
            boolean allowed,
            long remaining,
            long retryAfterMs,
            int limit
    ) {
        /** Convenience: seconds to wait, rounded up (for {@code Retry-After} header). */
        public long retryAfterSeconds() {
            return (retryAfterMs + 999) / 1000;
        }
    }

    /**
     * Checks whether the given identifier is within its rate limit using the
     * Token Bucket algorithm.
     *
     * <p>The bucket has:
     * <ul>
     *   <li>{@code capacity}  — maximum burst size (tokens that can accumulate while idle)</li>
     *   <li>{@code refillRpm} — steady refill rate in requests-per-minute</li>
     * </ul>
     *
     * <p>For a simple "N requests per minute" limit, set
     * {@code capacity = N} and {@code refillRpm = N}.
     * For a "20 burst, but only 10 sustained per minute" limit, set
     * {@code capacity = 20} and {@code refillRpm = 10}.
     *
     * @param namespace  logical name for this rate-limit (e.g. {@code "auth:ip"})
     * @param identifier value being throttled (e.g. client IP, email address, API key ID)
     * @param capacity   bucket capacity — maximum tokens that can accumulate (burst size)
     * @param refillRpm  token refill rate in tokens per minute (steady-state allowed rate)
     * @return a {@link TokenBucketResult} describing the outcome
     */
    public TokenBucketResult tryConsume(String namespace, String identifier,
                                        int capacity, int refillRpm) {
        if (capacity <= 0 || refillRpm <= 0) {
            return new TokenBucketResult(true, capacity, 0L, capacity);
        }
        try {
            String key   = "crescendo:ratelimit:" + namespace + KEY_SEPARATOR + sanitize(identifier);
            long   nowMs = System.currentTimeMillis();
            // TTL = time to fully refill an empty bucket (in ms), with 10 s headroom
            long   ttlMs = (long) capacity * 60_000L / refillRpm + 10_000L;

            List<Long> result = redisTemplate.execute(
                    TOKEN_BUCKET_SCRIPT,
                    List.of(key),
                    String.valueOf(capacity),
                    String.valueOf(refillRpm),
                    String.valueOf(nowMs),
                    String.valueOf(ttlMs)
            );

            if (result == null || result.size() < 2) {
                log.warn("TokenBucket: unexpected null/short result from Lua for key={}", key);
                return new TokenBucketResult(true, capacity, 0L, capacity); // fail-open
            }

            boolean allowed = result.get(0) == 1L;
            long    second  = result.get(1);

            if (allowed) {
                return new TokenBucketResult(true, second, 0L, capacity);
            } else {
                return new TokenBucketResult(false, 0L, second, capacity);
            }

        } catch (Exception ex) {
            log.warn("RateLimiter Redis operation failed (failing open): {}", ex.getMessage());
            return new TokenBucketResult(true, capacity, 0L, capacity);
        }
    }

    /**
     * Convenience overload that mirrors the old {@code isRateLimited} boolean signature.
     *
     * <p>Sets {@code capacity = maxRequests} and {@code refillRpm = maxRequests}
     * (i.e. a bucket that allows up to {@code maxRequests} per minute with a burst
     * equal to the per-minute allowance — equivalent behaviour to the old fixed window
     * but without the window-edge double-burst).
     *
     * <p>Kept for callers ({@link AuthRateLimitingFilter}, {@link ApiKeyAuthenticationFilter})
     * that only need a boolean decision and do not attach rate-limit response headers.
     *
     * @param namespace   logical name for this rate-limit
     * @param identifier  value being throttled
     * @param maxRequests maximum allowed requests per minute (= capacity = refillRpm)
     * @return {@code true} if the caller has EXCEEDED the limit and should be rejected
     */
    public boolean isRateLimited(String namespace, String identifier, int maxRequests) {
        return !tryConsume(namespace, identifier, maxRequests, maxRequests).allowed();
    }

    /**
     * Sanitizes an identifier to prevent Redis key injection.
     * Colons are replaced since they are used as namespace separators.
     */
    private String sanitize(String identifier) {
        if (identifier == null) return "unknown";
        return identifier.replace(":", "_").trim();
    }
}
