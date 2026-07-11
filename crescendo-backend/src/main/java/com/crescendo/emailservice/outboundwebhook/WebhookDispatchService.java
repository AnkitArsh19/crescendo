package com.crescendo.emailservice.outboundwebhook;

import tools.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class WebhookDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookDispatchService.class);

    // Exponential backoff retry delays (5 attempts: 1 min, 5 min, 30 min, 2 h, 8 h)
    private static final long[] RETRY_DELAYS_MINUTES = { 1, 5, 30, 120, 480 };

    private final WebhookSubscriptionRepository subscriptionRepo;
    private final WebhookDeliveryLogRepository deliveryLogRepo;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public WebhookDispatchService(WebhookSubscriptionRepository subscriptionRepo,
            WebhookDeliveryLogRepository deliveryLogRepo,
            ObjectMapper objectMapper) {
        this.subscriptionRepo = subscriptionRepo;
        this.deliveryLogRepo = deliveryLogRepo;
        this.objectMapper = objectMapper;

        // Apache HttpClient5 with:
        //   • redirect-following DISABLED (3xx to internal addresses bypasses SSRF check)
        //   • 5-second connect timeout
        //   • 10-second response timeout
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(5_000, TimeUnit.MILLISECONDS)
                .setResponseTimeout(10_000, TimeUnit.MILLISECONDS)
                .build();
        HttpClient httpClient = HttpClients.custom()
                .disableRedirectHandling()
                .setDefaultRequestConfig(requestConfig)
                .build();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Dispatch an event to all interested subscribers.
     * Called by the domain event consumer when an email event fires.
     */
    public void dispatch(UUID userId, String eventType, Map<String, Object> payload) {
        List<WebhookSubscription> subscriptions = subscriptionRepo.findByUserId(userId);
        for (WebhookSubscription sub : subscriptions) {
            if (sub.getSubscribedEvents().contains(eventType)) {
                WebhookDeliveryLog log = new WebhookDeliveryLog(
                        UUID.randomUUID(), sub.getId(), eventType, payload, Instant.now());
                deliveryLogRepo.save(log);
            }
        }
    }

    /**
     * Scheduled poller: claim and dispatch pending webhook deliveries.
     *
     * Row-claim locking: claimPendingForDispatch uses SELECT … FOR UPDATE SKIP LOCKED,
     * so two concurrent service instances will each claim disjoint rows — preventing
     * duplicate delivery to the customer's endpoint.
     */
    @Scheduled(fixedDelay = 10_000)
    public void processPendingWebhooks() {
        // Claim max 50 rows per tick, exclusive lock per row (SKIP LOCKED)
        List<WebhookDeliveryLog> pending = deliveryLogRepo.claimPendingForDispatch(
                Instant.now(), PageRequest.of(0, 50));

        for (WebhookDeliveryLog delivery : pending) {
            WebhookSubscription sub = subscriptionRepo.findById(delivery.getSubscriptionId()).orElse(null);
            if (sub == null) {
                delivery.setStatus(WebhookDeliveryLog.DeliveryStatus.FAILED);
                deliveryLogRepo.save(delivery);
                continue;
            }
            attemptDelivery(delivery, sub);
        }
    }

    private void attemptDelivery(WebhookDeliveryLog delivery, WebhookSubscription sub) {
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);

        // --- Dispatch-time SSRF guard ---
        // Re-resolved here (not just at subscription creation) to prevent DNS-rebinding.
        // NOTE: two separate DNS resolutions still occur (here and inside RestTemplate).
        // Complete pinning via a custom DnsResolver is a future hardening task.
        try {
            assertPublicUrl(sub.getUrl());
        } catch (IllegalArgumentException e) {
            logger.warn("[webhook] SSRF guard rejected URL {} for delivery {}: {}",
                    sub.getUrl(), delivery.getId(), e.getMessage());
            delivery.setStatus(WebhookDeliveryLog.DeliveryStatus.FAILED);
            delivery.setNextRetryAt(null);
            deliveryLogRepo.save(delivery);
            return;
        }

        long start = System.currentTimeMillis();

        try {
            String payloadStr = objectMapper.writeValueAsString(delivery.getPayload());
            String signature = generateHmacSha256(payloadStr, sub.getSigningSecret());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Crescendo-Signature", signature);
            headers.set("X-Crescendo-Event", delivery.getEventType());
            headers.set("X-Crescendo-Delivery", delivery.getId().toString());

            HttpEntity<String> entity = new HttpEntity<>(payloadStr, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    sub.getUrl(), HttpMethod.POST, entity, String.class);

            delivery.setResponseCode(response.getStatusCode().value());
            delivery.setLatencyMs(System.currentTimeMillis() - start);

            if (response.getStatusCode().is2xxSuccessful()) {
                delivery.setStatus(WebhookDeliveryLog.DeliveryStatus.DELIVERED);
            } else {
                scheduleRetry(delivery);
            }
        } catch (Exception e) {
            logger.warn("[webhook] Failed to deliver webhook {} to {}: {}",
                    delivery.getId(), sub.getUrl(), e.getMessage());
            delivery.setLatencyMs(System.currentTimeMillis() - start);
            scheduleRetry(delivery);
        }

        deliveryLogRepo.save(delivery);
    }

    private void scheduleRetry(WebhookDeliveryLog delivery) {
        int attempt = delivery.getAttemptCount();
        // attempt is 1-indexed after increment; array is 0-indexed
        if (attempt - 1 >= RETRY_DELAYS_MINUTES.length) {
            delivery.setStatus(WebhookDeliveryLog.DeliveryStatus.FAILED);
            delivery.setNextRetryAt(null);
        } else {
            long delayMinutes = RETRY_DELAYS_MINUTES[attempt - 1];
            delivery.setNextRetryAt(Instant.now().plus(Duration.ofMinutes(delayMinutes)));
        }
    }

    /**
     * Dispatch-time SSRF guard. Validates that the URL resolves to a public IP.
     * Also called at subscription-creation time in PublicWebhookController for fast-fail.
     *
     * Known residual risk: two separate DNS resolutions still occur (this check + RestTemplate's
     * own lookup). A DNS-rebinding attacker with TTL=0 can still exploit the window between them.
     * Complete mitigation requires pinning the resolved IP to the connection via a custom DnsResolver.
     */
    private void assertPublicUrl(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("Missing hostname in webhook URL");
            }
            InetAddress addr = InetAddress.getByName(host);
            if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()
                    || addr.isLinkLocalAddress() || addr.isAnyLocalAddress()
                    || isUniqueLocal(addr)) {
                throw new IllegalArgumentException("Webhook URL resolves to a non-public address: " + addr);
            }
        } catch (URISyntaxException | UnknownHostException e) {
            throw new IllegalArgumentException("Cannot validate webhook URL: " + e.getMessage());
        }
    }

    /** IPv6 fc00::/7 unique-local range — isSiteLocalAddress() does not cover this. */
    private boolean isUniqueLocal(InetAddress addr) {
        byte[] b = addr.getAddress();
        return b.length == 16 && (b[0] & 0xFE) == 0xFC;
    }

    private String generateHmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC", e);
        }
    }
}
