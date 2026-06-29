package com.crescendo.shared.domain.valueobject;

import jakarta.persistence.Embeddable;
import java.util.regex.Pattern;

/**
 * Value Object representing an IP address (IPv4 or IPv6).
 * Immutable and self-validating.
 */
@Embeddable
public record IpAddress(String value) {

    // Basic regex for IPv4 and IPv6 structure (not strictly RFC compliant but enough to prevent garbage)
    private static final Pattern IPV4_PATTERN = Pattern.compile("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");
    private static final Pattern IPV6_PATTERN = Pattern.compile("^([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])$");

    public IpAddress {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("IP Address cannot be null or blank");
        }
        value = value.trim();
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }
        
        if (!IPV4_PATTERN.matcher(value).matches() && !IPV6_PATTERN.matcher(value).matches()) {
             // We log this or handle it, but for a value object, if it's completely malformed, reject it.
             // Sometimes proxies forward weird comma separated lists, those should be parsed before reaching here.
             throw new IllegalArgumentException("Invalid IP Address format: " + value);
        }
    }

    public static IpAddress of(String value) {
        if (value == null) return null;
        return new IpAddress(value);
    }
    
    /**
     * Checks if two IP addresses are completely different (could indicate session hijacking if changed drastically).
     * For a real implementation, you'd want a geo-IP check, but for now, simple string equality.
     */
    public boolean isSuspiciouslyDifferentFrom(IpAddress other) {
        if (other == null) return false;
        return !this.value.equals(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
