package com.crescendo.shared.domain.valueobject;

import jakarta.persistence.Embeddable;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Value Object representing a guest session identifier.
 * Used to track guest users who try the app without login.
 * Immutable and self-validating.
 */
@Embeddable
public record GuestSessionId(String value) {

    private static final int MIN_LENGTH = 20;
    private static final int MAX_LENGTH = 100;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public GuestSessionId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Guest session ID cannot be null or blank");
        }
        value = value.trim();
        if (value.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Guest session ID must be at least " + MIN_LENGTH + " characters");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Guest session ID cannot exceed " + MAX_LENGTH + " characters");
        }
    }

    /**
     * Factory method for creating GuestSessionId from string.
     */
    public static GuestSessionId of(String value) {
        return new GuestSessionId(value);
    }

    /**
     * Generate a new cryptographically secure guest session ID.
     */
    public static GuestSessionId generate() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String id = "guest_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new GuestSessionId(id);
    }

    @Override
    public String toString() {
        return value;
    }
}
