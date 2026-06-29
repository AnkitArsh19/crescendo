package com.crescendo.security;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Sanitizes data payloads before they are persisted to execution logs (StepRun
 * inputs/outputs).
 *
 * <p>
 * Inspired by n8n's {@code OutputRedactor} and {@code error-formatting.ts},
 * which use
 * regex-based redaction to mask secrets before they reach logs or the UI.
 *
 * <p>
 * Three layers of redaction:
 * <ol>
 * <li><b>Key-based:</b> Any JSON map entry whose key matches a sensitive
 * pattern
 * has its value replaced with {@code [REDACTED]}.</li>
 * <li><b>URL credential:</b> Inline credentials in URLs
 * (e.g. {@code https://user:password@host}) are replaced with
 * {@code https://[REDACTED]:[REDACTED]@host}.</li>
 * <li><b>Inline token:</b> Common inline patterns like
 * {@code Authorization: Bearer <token>},
 * {@code token=<value>}, {@code api_key=<value>} in string values are
 * masked.</li>
 * </ol>
 *
 * <p>
 * Redaction is applied recursively — nested maps and list values are also
 * scrubbed.
 */
@Service
public class DataSanitizationService {

    public static final String REDACTED = "[REDACTED]";

    /**
     * Sensitive key patterns (case-insensitive substring match).
     * Any map key that contains one of these words will have its value replaced.
     */
    private static final Set<String> SENSITIVE_KEY_PATTERNS = Set.of(
            "password", "passwd",
            "secret",
            "token",
            "api_key", "apikey",
            "access_key",
            "private_key",
            "client_secret",
            "authorization",
            "auth_header",
            "credential",
            "private",
            "key" // broad catch-all — will match "api_key", "ssh_key", etc.
    );

    /**
     * Keys that are safe to pass through even though they might contain a word from
     * SENSITIVE_KEY_PATTERNS. (e.g. "key_name", "keyboard_shortcut")
     */
    private static final Set<String> SAFE_KEY_ALLOWLIST = Set.of(
            "keyboard", "key_name", "key_id", "lock_key");

    // Matches: https://user:password@host → https://[REDACTED]:[REDACTED]@host
    private static final Pattern URL_CREDENTIALS_PATTERN = Pattern.compile(
            "(https?://)([^:@/\\s]+):([^@/\\s]+)@");

    // Matches: Authorization: Bearer <token> or Authorization: Basic <token>
    private static final Pattern AUTH_HEADER_PATTERN = Pattern.compile(
            "(?i)(authorization[:\\s\"']*)(bearer|basic)\\s+([A-Za-z0-9+/._\\-=]+)");

    // Matches: token=<value>, api_key=<value>, access_token=<value>,
    // client_secret=<value>
    private static final Pattern KEY_VALUE_SECRET_PATTERN = Pattern.compile(
            "(?i)(token|api_key|access_token|client_secret|password|secret)=([^&\\s\"']+)");

    // Matches: "access_token": "value" or 'secret': 'value' in raw JSON/YAML
    // strings
    private static final Pattern QUOTED_SECRET_PATTERN = Pattern.compile(
            "(?i)(\"(?:access_token|client_secret|password|secret|api_key|authorization|token)\"\\s*:\\s*\")([^\"]+)(\")");

    // -------------------------------------------------------------------------
    // PUBLIC API
    // -------------------------------------------------------------------------

    /**
     * Sanitizes a {@code Map<String, Object>} payload in place, returning a NEW map
     * with
     * sensitive values redacted. The original map is NOT modified.
     *
     * @param payload the raw input or output data from a workflow step
     * @return a new map with sensitive values replaced by {@value #REDACTED}
     */
    public Map<String, Object> sanitize(Map<String, Object> payload) {
        if (payload == null)
            return null;
        return sanitizeMap(payload);
    }

    /**
     * Sanitizes a plain {@code String} value — useful for error messages that may
     * contain inline credentials or bearer tokens.
     *
     * @param value the raw string to sanitize
     * @return the sanitized string with sensitive patterns masked
     */
    public String sanitizeString(String value) {
        if (value == null || value.isBlank())
            return value;
        return applyStringRedaction(value);
    }

    // -------------------------------------------------------------------------
    // PRIVATE HELPERS
    // -------------------------------------------------------------------------

    private Map<String, Object> sanitizeMap(Map<?, ?> map) {
        Map<String, Object> result = new HashMap<>(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();

            if (isSensitiveKey(key)) {
                result.put(key, REDACTED);
            } else {
                result.put(key, sanitizeValue(value));
            }
        }
        return result;
    }

    private Object sanitizeValue(Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            return sanitizeMap(mapValue);
        } else if (value instanceof List<?> listValue) {
            return sanitizeList(listValue);
        } else if (value instanceof String strValue) {
            return applyStringRedaction(strValue);
        }
        return value;
    }

    private List<Object> sanitizeList(List<?> list) {
        List<Object> result = new ArrayList<>(list.size());
        for (Object item : list) {
            result.add(sanitizeValue(item));
        }
        return result;
    }

    /**
     * Determines if a map key is considered sensitive.
     * Case-insensitive substring match against {@link #SENSITIVE_KEY_PATTERNS}.
     */
    private boolean isSensitiveKey(String key) {
        if (key == null)
            return false;
        String lower = key.toLowerCase();

        // Check allowlist first
        for (String safe : SAFE_KEY_ALLOWLIST) {
            if (lower.contains(safe))
                return false;
        }

        for (String pattern : SENSITIVE_KEY_PATTERNS) {
            if (lower.contains(pattern))
                return true;
        }
        return false;
    }

    /**
     * Applies all regex-based redaction patterns to a string value.
     * Runs multiple passes so overlapping patterns are all caught.
     */
    private String applyStringRedaction(String value) {
        // Pass 1: URL credentials
        value = URL_CREDENTIALS_PATTERN.matcher(value)
                .replaceAll("$1[REDACTED]:[REDACTED]@");

        // Pass 2: Authorization header values
        value = AUTH_HEADER_PATTERN.matcher(value)
                .replaceAll("$1$2 [REDACTED]");

        // Pass 3: Key-value pair secrets (token=abc, api_key=xyz)
        value = KEY_VALUE_SECRET_PATTERN.matcher(value)
                .replaceAll("$1=[REDACTED]");

        // Pass 4: Quoted JSON secrets ("access_token": "value")
        value = QUOTED_SECRET_PATTERN.matcher(value)
                .replaceAll("$1[REDACTED]$3");

        return value;
    }
}
