package com.crescendo.shared.domain.valueobject;

import jakarta.persistence.Embeddable;
import java.util.regex.Pattern;

/**
 * Value Object representing an action key for workflow steps.
 * Identifies specific actions like "send_email", "create_row", etc.
 * Immutable and self-validating.
 */
@Embeddable
public record ActionKey(String value) {

    private static final Pattern ACTION_KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9_-]{1,119}$");
    private static final int MAX_LENGTH = 120;

    public ActionKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Action key cannot be null or blank");
        }
        value = value.trim().toLowerCase();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Action key cannot exceed " + MAX_LENGTH + " characters");
        }
        if (!ACTION_KEY_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Action key must start with a letter and contain only lowercase letters, numbers, underscores, and hyphens");
        }
    }

    /**
     * Factory method for creating ActionKey from string.
     */
    public static ActionKey of(String value) {
        return new ActionKey(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
