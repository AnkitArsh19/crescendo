package com.crescendo.publicapi.audit;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * Filter that enforces Idempotency-Key handling on POST endpoints in the public API.
 * Ensures duplicate requests (e.g. from network retries) do not result in duplicate state mutations.
 * Returns HTTP 409 Conflict if the same key is reused with a different payload.
 */
@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String REDIS_PREFIX = "crescendo:idempotency:";

    public IdempotencyFilter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only apply to POST requests on the public API
        return !request.getRequestURI().startsWith("/api/v1/") || !HttpMethod.POST.name().equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String idempotencyKey = request.getHeader("Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, 1024 * 1024);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        // We need to read the payload to hash it.
        // ContentCachingRequestWrapper caches content *as it is read*, so we can't hash it before the controller runs.
        // Instead, we will intercept *before* by buffering, or we can just let it run if it's not in cache.
        // Wait, if it's already in Redis, we don't need the request payload immediately unless we want to compare it.
        
        String redisKey = REDIS_PREFIX + idempotencyKey;
        String cachedHash = redisTemplate.opsForHash().get(redisKey, "hash") != null ? (String) redisTemplate.opsForHash().get(redisKey, "hash") : null;
        
        if (cachedHash != null) {
            // It's a replay. But we must verify the payload hasn't changed.
            // To do this, we must read the body. For ContentCachingRequestWrapper, we must actually consume the stream.
            byte[] body = requestWrapper.getInputStream().readAllBytes();
            String currentHash = hash(body);
            
            if (!currentHash.equals(cachedHash)) {
                // The client reused the key with a genuinely different request payload
                sendConflictResponse(responseWrapper);
                return;
            }

            // Return cached response
            String cachedStatus = (String) redisTemplate.opsForHash().get(redisKey, "status");
            String cachedBody = (String) redisTemplate.opsForHash().get(redisKey, "body");
            String cachedContentType = (String) redisTemplate.opsForHash().get(redisKey, "contentType");

            responseWrapper.setStatus(Integer.parseInt(cachedStatus));
            if (cachedContentType != null) {
                responseWrapper.setContentType(cachedContentType);
            }
            responseWrapper.getWriter().write(cachedBody);
            responseWrapper.copyBodyToResponse();
            return;
        }

        // Execute the controller
        filterChain.doFilter(requestWrapper, responseWrapper);

        // Cache the response and the request hash for 24 hours (only if it's a successful 2xx response)
        if (responseWrapper.getStatus() >= 200 && responseWrapper.getStatus() < 300) {
            byte[] requestBody = requestWrapper.getContentAsByteArray();
            String requestHash = hash(requestBody);
            
            byte[] responseBody = responseWrapper.getContentAsByteArray();
            String responseStr = new String(responseBody, StandardCharsets.UTF_8);

            redisTemplate.opsForHash().put(redisKey, "hash", requestHash);
            redisTemplate.opsForHash().put(redisKey, "status", String.valueOf(responseWrapper.getStatus()));
            redisTemplate.opsForHash().put(redisKey, "body", responseStr);
            if (responseWrapper.getContentType() != null) {
                redisTemplate.opsForHash().put(redisKey, "contentType", responseWrapper.getContentType());
            }
            redisTemplate.expire(redisKey, Duration.ofHours(24));
        }

        responseWrapper.copyBodyToResponse();
    }

    private void sendConflictResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.CONFLICT.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> error = Map.of(
                "type", "conflict_error",
                "message", "The Idempotency-Key was reused with a different request payload.",
                "status", HttpStatus.CONFLICT.value()
        );
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    private String hash(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(content != null ? content : new byte[0]);
            return Base64.getEncoder().encodeToString(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
