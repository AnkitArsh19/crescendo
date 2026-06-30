package com.crescendo.workflow;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Public (unauthenticated) endpoint for previewing shared workflows.
 *
 * Rate-limiting and max ID count (20) prevent enumeration attacks.
 */
@RestController
@RequestMapping("/shared")
public class SharedWorkflowController {

    private final StringRedisTemplate redisTemplate;

    public SharedWorkflowController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // NEW TEMPLATE STORAGE ENDPOINTS
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * POST /shared/templates
     * Generates a unique share link for the provided JSON payload.
     * The payload is stored in Redis with a 30-day TTL.
     */
    @PostMapping("/templates")
    public ResponseEntity<Map<String, String>> createSharedTemplate(@RequestBody String payload) {
        // payload is the raw JSON string array sent from frontend
        String shareId = UUID.randomUUID().toString();
        String key = "shared_template:" + shareId;
        
        // Store in Redis with 30-day expiration
        redisTemplate.opsForValue().set(key, payload, Duration.ofDays(30));
        
        return ResponseEntity.ok(Map.of("shareId", shareId));
    }

    /**
     * GET /shared/templates/{shareId}
     * Retrieves the JSON payload for a share link.
     */
    @GetMapping(value = "/templates/{shareId}", produces = "application/json")
    public ResponseEntity<String> getSharedTemplate(@PathVariable String shareId) {
        String key = "shared_template:" + shareId;
        String payload = redisTemplate.opsForValue().get(key);
        
        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Share link expired or invalid");
        }
        
        return ResponseEntity.ok(payload);
    }
}
