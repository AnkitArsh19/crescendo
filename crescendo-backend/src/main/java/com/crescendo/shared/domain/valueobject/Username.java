package com.crescendo.shared.domain.valueobject;

import jakarta.persistence.Embeddable;
import java.util.regex.Pattern;

/**
 * Value Object representing a valid username (display name).
 * Allows letters, numbers, spaces, underscores, hyphens, and periods.
 * Preserves the user's chosen casing and spacing (e.g. "Ankit Arsh").
 * Immutable and self-validating.
 */
@Embeddable
public record Username(String value) {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N} _.'-]{1,100}$");
    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 100;

    public Username {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }
        value = value.strip();
        if (value.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Username must be at least " + MIN_LENGTH + " character");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Username cannot exceed " + MAX_LENGTH + " characters");
        }
        if (!USERNAME_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Username contains invalid characters");
        }
    }

    /**
     * Factory method for creating Username from string.
     */
    public static Username of(String value) {
        return new Username(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
