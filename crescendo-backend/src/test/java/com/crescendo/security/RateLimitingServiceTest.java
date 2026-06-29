package com.crescendo.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void isRateLimited_firstRequest_incrementsAndSetsExpire() {
        String namespace = "auth:ip";
        String identifier = "192.168.1.1";
        Duration window = Duration.ofMinutes(1);

        when(valueOperations.increment(anyString())).thenReturn(1L);

        boolean isLimited = rateLimitingService.isRateLimited(namespace, identifier, 5, window);

        assertFalse(isLimited);
        verify(redisTemplate).expire(eq("crescendo:ratelimit:auth:ip:192.168.1.1"), eq(window));
    }

    @Test
    void isRateLimited_subsequentRequest_doesNotSetExpire() {
        String namespace = "auth:ip";
        String identifier = "192.168.1.1";
        Duration window = Duration.ofMinutes(1);

        when(valueOperations.increment(anyString())).thenReturn(3L);

        boolean isLimited = rateLimitingService.isRateLimited(namespace, identifier, 5, window);

        assertFalse(isLimited);
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void isRateLimited_exceedsLimit_returnsTrue() {
        String namespace = "auth:ip";
        String identifier = "192.168.1.1";
        Duration window = Duration.ofMinutes(1);

        // Max requests is 5. This is the 6th request.
        when(valueOperations.increment(anyString())).thenReturn(6L);

        boolean isLimited = rateLimitingService.isRateLimited(namespace, identifier, 5, window);

        assertTrue(isLimited);
    }

    @Test
    void isRateLimited_sanitizesColonsFromIdentifier() {
        String namespace = "auth:email";
        String identifier = "user:name@example.com";
        Duration window = Duration.ofMinutes(1);

        when(valueOperations.increment(anyString())).thenReturn(1L);

        rateLimitingService.isRateLimited(namespace, identifier, 5, window);

        verify(valueOperations).increment("crescendo:ratelimit:auth:email:user_name@example.com");
    }
}
