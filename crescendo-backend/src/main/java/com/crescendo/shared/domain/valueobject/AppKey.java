package com.crescendo.shared.domain.valueobject;

import jakarta.persistence.Embeddable;
import java.util.regex.Pattern;

/**
 * Value Object representing an application key.
 * Used to identify apps like gmail, slack, notion, etc.
 * Immutable and self-validating.
 */
@Embeddable
public record AppKey(String value) {

    private static final Pattern APP_KEY_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]{1,99}$");
    private static final int MAX_LENGTH = 100;

    public AppKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("App key cannot be null or blank");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("App key cannot exceed " + MAX_LENGTH + " characters");
        }
        if (!APP_KEY_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("App key must start with a letter and contain only letters, numbers, underscores, and hyphens");
        }
    }

    /**
     * Factory method for creating AppKey from string.
     */
    public static AppKey of(String value) {
        return new AppKey(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
