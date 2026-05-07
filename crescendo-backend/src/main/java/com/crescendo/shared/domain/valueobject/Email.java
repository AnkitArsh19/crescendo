package com.crescendo.shared.domain.valueobject;

import jakarta.persistence.Embeddable;
import java.util.regex.Pattern;

/**
 * Value Object representing a valid email address.
 * Immutable and self-validating - ensures email is always in valid format.
 */
@Embeddable
public record Email(String value) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final int MAX_LENGTH = 320;

    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
        value = value.trim().toLowerCase();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Email cannot exceed " + MAX_LENGTH + " characters");
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }

    /**
     * Factory method for creating Email from string.
     */
    public static Email of(String value) {
        return new Email(value);
    }

    /**
     * Returns the domain part of the email.
     */
    public String domain() {
        return value.substring(value.indexOf('@') + 1);
    }

    /**
     * Returns the local part of the email (before @).
     */
    public String localPart() {
        return value.substring(0, value.indexOf('@'));
    }

    @Override
    public String toString() {
        return value;
    }
}
