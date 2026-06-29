package com.crescendo.shared.infrastructure.lock;

import com.crescendo.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DistributedLockServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private DistributedLockService lockService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private String testLockKey;

    @BeforeEach
    void setUp() {
        testLockKey = "test-lock-" + UUID.randomUUID().toString();
        redisTemplate.delete("crescendo:lock:" + testLockKey);
    }

    @Test
    void tryLock_acquiresLockSuccessfully() {
        Optional<String> token = lockService.tryLock(testLockKey, 10000);
        
        assertTrue(token.isPresent());
        assertTrue(lockService.isLocked(testLockKey));
    }

    @Test
    void tryLock_failsWhenAlreadyLocked() {
        Optional<String> token1 = lockService.tryLock(testLockKey, 10000);
        assertTrue(token1.isPresent());

        Optional<String> token2 = lockService.tryLock(testLockKey, 10000);
        assertFalse(token2.isPresent(), "Second attempt to lock should fail");
    }

    @Test
    void unlock_succeedsWithCorrectToken() {
        Optional<String> token = lockService.tryLock(testLockKey, 10000);
        assertTrue(token.isPresent());

        boolean unlocked = lockService.unlock(testLockKey, token.get());
        assertTrue(unlocked, "Unlock should succeed with correct token");
        assertFalse(lockService.isLocked(testLockKey), "Lock should be released");
    }

    @Test
    void unlock_failsWithIncorrectToken() {
        Optional<String> token = lockService.tryLock(testLockKey, 10000);
        assertTrue(token.isPresent());

        boolean unlocked = lockService.unlock(testLockKey, "wrong-token");
        assertFalse(unlocked, "Unlock should fail with incorrect token");
        assertTrue(lockService.isLocked(testLockKey), "Lock should still be held");
    }

    @Test
    void extend_succeedsWithCorrectToken() {
        Optional<String> token = lockService.tryLock(testLockKey, 10000);
        assertTrue(token.isPresent());

        boolean extended = lockService.extend(testLockKey, token.get(), 20000);
        assertTrue(extended, "Extend should succeed with correct token");
    }

    @Test
    void extend_failsWithIncorrectToken() {
        Optional<String> token = lockService.tryLock(testLockKey, 10000);
        assertTrue(token.isPresent());

        boolean extended = lockService.extend(testLockKey, "wrong-token", 20000);
        assertFalse(extended, "Extend should fail with incorrect token");
    }
}
