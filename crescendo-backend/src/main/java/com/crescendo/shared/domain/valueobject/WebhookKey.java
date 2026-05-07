package com.crescendo.shared.domain.valueobject;

import jakarta.persistence.Embeddable;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Value Object representing a unique webhook key.
 * Used to identify webhooks for incoming trigger requests.
 * Immutable and self-validating.
 */
@Embeddable
public record WebhookKey(String value) {

    private static final int MIN_LENGTH = 20;
    private static final int MAX_LENGTH = 100;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public WebhookKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Webhook key cannot be null or blank");
        }
        value = value.trim();
        if (value.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Webhook key must be at least " + MIN_LENGTH + " characters");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Webhook key cannot exceed " + MAX_LENGTH + " characters");
        }
    }

    /**
     * Factory method for creating WebhookKey from string.
     */
    public static WebhookKey of(String value) {
        return new WebhookKey(value);
    }

    /**
     * Generate a new cryptographically secure webhook key.
     */
    public static WebhookKey generate() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String key = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new WebhookKey(key);
    }

    @Override
    public String toString() {
        return value;
    }
}
