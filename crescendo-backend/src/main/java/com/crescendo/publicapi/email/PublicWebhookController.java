package com.crescendo.publicapi.email;

import com.crescendo.emailservice.outboundwebhook.WebhookSubscription;
import com.crescendo.emailservice.outboundwebhook.WebhookSubscriptionRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static com.crescendo.security.AuthenticatedUser.userId;
import static com.crescendo.publicapi.PublicApiScopes.WEBHOOK_READ;
import static com.crescendo.publicapi.PublicApiScopes.WEBHOOK_WRITE;
import static com.crescendo.publicapi.PublicApiScopes.require;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "Public API for managing outbound webhook subscriptions")
public class PublicWebhookController {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final WebhookSubscriptionRepository subscriptionRepo;
    private final StringRedisTemplate redisTemplate;

    public PublicWebhookController(WebhookSubscriptionRepository subscriptionRepo,
                                   StringRedisTemplate redisTemplate) {
        this.subscriptionRepo = subscriptionRepo;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping
    public ResponseEntity<PublicWebhookDto.PublicWebhookResponse> createSubscription(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PublicWebhookDto.CreateWebhookRequest req,
            Authentication auth) {
        require(auth, WEBHOOK_WRITE);
        UUID userId = userId(auth);

        // --- Idempotency enforcement ---
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String redisKey = "public-api:idempotency:webhook:create:" + userId + ":" + idempotencyKey;
            Boolean claimed = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofHours(24));
            if (!Boolean.TRUE.equals(claimed)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Duplicate Idempotency-Key: a webhook subscription was already created with this key.");
            }
        }

        // --- First-fail-fast SSRF guard at creation time ---
        assertPublicUrl(req.url());

        byte[] secretBytes = new byte[32];
        RANDOM.nextBytes(secretBytes);
        String secret = "whsec_" + Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);

        WebhookSubscription sub = new WebhookSubscription(
                UUID.randomUUID(), userId, req.url(), secret, req.subscribedEvents());

        subscriptionRepo.save(sub);

        // Return full response with signing secret — only time it is ever exposed
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToFullResponse(sub));
    }

    @GetMapping
    public ResponseEntity<List<PublicWebhookDto.PublicWebhookListResponse>> listSubscriptions(Authentication auth) {
        require(auth, WEBHOOK_READ);
        List<WebhookSubscription> subscriptions = subscriptionRepo.findByUserId(userId(auth));
        // Use list response — signingSecret intentionally omitted
        return ResponseEntity.ok(subscriptions.stream().map(this::mapToListResponse).collect(Collectors.toList()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscription(
            @PathVariable UUID id,
            Authentication auth) {
        require(auth, WEBHOOK_WRITE);
        UUID userId = userId(auth);

        WebhookSubscription sub = subscriptionRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!sub.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        subscriptionRepo.delete(sub);
        return ResponseEntity.noContent().build();
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    /**
     * First-fail-fast SSRF guard. Validates the URL at subscription-creation time.
     * NOTE: WebhookDispatchService also validates at dispatch time to prevent DNS-rebinding.
     * These two checks are intentionally redundant.
     */
    private void assertPublicUrl(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook URL must use http or https");
            }
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook URL must contain a valid hostname");
            }
            InetAddress addr = InetAddress.getByName(host);
            if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()
                    || addr.isLinkLocalAddress() || addr.isAnyLocalAddress()
                    || isUniqueLocal(addr)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Webhook URL must point to a publicly reachable address");
            }
        } catch (URISyntaxException | UnknownHostException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook URL: " + e.getMessage());
        }
    }

    /** IPv6 fc00::/7 unique-local range — isSiteLocalAddress() does not cover this. */
    private boolean isUniqueLocal(InetAddress addr) {
        byte[] b = addr.getAddress();
        return b.length == 16 && (b[0] & 0xFE) == 0xFC;
    }

    private PublicWebhookDto.PublicWebhookResponse mapToFullResponse(WebhookSubscription sub) {
        return new PublicWebhookDto.PublicWebhookResponse(
                sub.getId().toString(),
                sub.getUrl(),
                sub.getSigningSecret(),
                sub.getSubscribedEvents(),
                sub.getCreatedAt());
    }

    private PublicWebhookDto.PublicWebhookListResponse mapToListResponse(WebhookSubscription sub) {
        return new PublicWebhookDto.PublicWebhookListResponse(
                sub.getId().toString(),
                sub.getUrl(),
                sub.getSubscribedEvents(),
                sub.getCreatedAt());
    }
}
