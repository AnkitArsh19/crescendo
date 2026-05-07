package com.crescendo.shared.domain.valueobject;

import jakarta.persistence.Embeddable;
import java.util.regex.Pattern;

/**
 * Value Object representing a valid domain name.
 * Immutable and self-validating - used for email service custom domains.
 */
@Embeddable
public record DomainName(String value) {

    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "^([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$"
    );

    private static final int MAX_LENGTH = 255;

    public DomainName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Domain name cannot be null or blank");
        }
        value = value.trim().toLowerCase();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Domain name cannot exceed " + MAX_LENGTH + " characters");
        }
        if (!DOMAIN_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid domain name format: " + value);
        }
    }

    /**
     * Factory method for creating DomainName from string.
     */
    public static DomainName of(String value) {
        return new DomainName(value);
    }

    /**
     * Returns the top-level domain (e.g., "com" from "example.com").
     */
    public String tld() {
        int lastDot = value.lastIndexOf('.');
        return value.substring(lastDot + 1);
    }

    @Override
    public String toString() {
        return value;
    }
}
