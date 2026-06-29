package com.crescendo.shared.infrastructure.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-based distributed locking using {@code SET key value NX EX}.
 *
 * Provides mutual exclusion across application instances for operations that
 * must not run concurrently (e.g., workflow execution, bulk mutations).
 *
 * Each lock has a unique owner token (generated per acquisition, not per thread)
 * to prevent accidental release by a different holder. Locks auto-expire after
 * the specified TTL to prevent deadlocks from crashed holders.
 *
 * <p><strong>Correctness guarantees:</strong></p>
 * <ul>
 *   <li>{@code unlock} and {@code extend} use Lua scripts for atomic compare-and-mutate,
 *       eliminating the TOCTOU race between checking ownership and deleting/expiring the key.</li>
 *   <li>Tokens are per-acquisition (not per-thread), so they work correctly across
 *       {@code @Async} boundaries and thread-pool reuse.</li>
 * </ul>
 *
 * Usage:
 * <pre>
 *   String lockKey = "workflow-execution:" + workflowId;
 *   Optional&lt;String&gt; token = lockService.tryLock(lockKey, 30_000);
 *   if (token.isPresent()) {
 *       try {
 *           // critical section
 *       } finally {
 *           lockService.unlock(lockKey, token.get());
 *       }
 *   }
 * </pre>
 */
@Service
public class DistributedLockService {

    private static final Logger logger = LoggerFactory.getLogger(DistributedLockService.class);
    private static final String LOCK_PREFIX = "crescendo:lock:";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Lua script for atomic compare-and-delete.
     * Returns 1 if the lock was released, 0 if not owned by the caller.
     */
    private static final RedisScript<Long> UNLOCK_SCRIPT = RedisScript.of(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end",
            Long.class
    );

    /**
     * Lua script for atomic compare-and-expire.
     * Returns 1 if the TTL was extended, 0 if not owned by the caller.
     */
    private static final RedisScript<Long> EXTEND_SCRIPT = RedisScript.of(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('pexpire', KEYS[1], tonumber(ARGV[2])) " +
            "else " +
            "  return 0 " +
            "end",
            Long.class
    );

    public DistributedLockService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Tries to acquire a distributed lock with the given key and TTL.
     *
     * @param key      logical lock name (e.g., "workflow-execution:abc-123")
     * @param ttlMillis auto-expiry in milliseconds to prevent deadlocks
     * @return the owner token if the lock was acquired, empty if already held
     */
    public Optional<String> tryLock(String key, long ttlMillis) {
        String redisKey = LOCK_PREFIX + key;
        String token = UUID.randomUUID().toString();

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, token, Duration.ofMillis(ttlMillis));

        if (Boolean.TRUE.equals(acquired)) {
            logger.debug("Acquired distributed lock: key={}, ttl={}ms, token={}", key, ttlMillis, token);
            return Optional.of(token);
        }

        logger.debug("Failed to acquire distributed lock (already held): key={}", key);
        return Optional.empty();
    }

    /**
     * Releases a distributed lock atomically, but only if the caller holds it.
     * Uses a Lua script to ensure the check-and-delete is a single atomic operation,
     * preventing the TOCTOU race where the lock could expire and be re-acquired
     * between a GET and DELETE.
     *
     * @param key   logical lock name
     * @param token the owner token returned by {@link #tryLock}
     * @return true if the lock was released, false if not held by this token
     */
    public boolean unlock(String key, String token) {
        String redisKey = LOCK_PREFIX + key;

        Long result = redisTemplate.execute(UNLOCK_SCRIPT, List.of(redisKey), token);

        if (result != null && result == 1L) {
            logger.debug("Released distributed lock: key={}", key);
            return true;
        }

        logger.warn("Cannot release lock (not owner or expired): key={}, token={}", key, token);
        return false;
    }

    /**
     * Checks if a lock is currently held (by any owner).
     *
     * @param key logical lock name
     * @return true if the lock exists in Redis
     */
    public boolean isLocked(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_PREFIX + key));
    }

    /**
     * Extends the TTL of a lock held by the given token, atomically.
     * Uses a Lua script to ensure the ownership check and TTL extension
     * happen in a single atomic operation.
     *
     * @param key       logical lock name
     * @param token     the owner token returned by {@link #tryLock}
     * @param ttlMillis new TTL in milliseconds
     * @return true if successfully extended, false if not the owner
     */
    public boolean extend(String key, String token, long ttlMillis) {
        String redisKey = LOCK_PREFIX + key;

        Long result = redisTemplate.execute(
                EXTEND_SCRIPT, List.of(redisKey), token, ttlMillis);

        if (result != null && result == 1L) {
            logger.debug("Extended distributed lock: key={}, newTtl={}ms", key, ttlMillis);
            return true;
        }

        logger.warn("Cannot extend lock (not owner or expired): key={}", key);
        return false;
    }
}
