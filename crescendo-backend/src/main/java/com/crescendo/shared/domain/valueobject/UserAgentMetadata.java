package com.crescendo.shared.domain.valueobject;

import jakarta.persistence.Embeddable;

/**
 * Value Object representing the raw User-Agent string.
 * Used for session auditing and device labeling.
 * Immutable and self-validating.
 */
@Embeddable
public record UserAgentMetadata(String value) {

    private static final int MAX_LENGTH = 500;

    public UserAgentMetadata {
        if (value != null) {
            value = value.trim();
            if (value.length() > MAX_LENGTH) {
                // Truncate if too long rather than failing, as User-Agents can be arbitrarily long and are just for metadata
                value = value.substring(0, MAX_LENGTH);
            }
        }
    }

    public static UserAgentMetadata of(String value) {
        if (value == null) return null;
        return new UserAgentMetadata(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
