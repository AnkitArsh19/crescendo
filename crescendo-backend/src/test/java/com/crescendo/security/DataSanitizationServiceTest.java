package com.crescendo.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DataSanitizationServiceTest {

    private DataSanitizationService service;

    @BeforeEach
    void setUp() {
        service = new DataSanitizationService();
    }

    @Test
    void sanitizeMap_redactsSensitiveKeys() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", "john_doe");
        payload.put("password", "supersecret123");
        payload.put("api_key", "sk_live_12345");
        payload.put("normal_field", "value");

        Map<String, Object> sanitized = service.sanitize(payload);

        assertEquals("john_doe", sanitized.get("username"));
        assertEquals("value", sanitized.get("normal_field"));
        assertEquals(DataSanitizationService.REDACTED, sanitized.get("password"));
        assertEquals(DataSanitizationService.REDACTED, sanitized.get("api_key"));
    }

    @Test
    void sanitizeMap_allowsSafeKeys() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("keyboard_layout", "qwerty"); // contains "key" but should be safe
        payload.put("key_name", "my_ssh_key");

        Map<String, Object> sanitized = service.sanitize(payload);

        assertEquals("qwerty", sanitized.get("keyboard_layout"));
        assertEquals("my_ssh_key", sanitized.get("key_name"));
    }

    @Test
    void sanitizeString_redactsUrlCredentials() {
        String url = "Connecting to https://admin:mypassword@database.com/db";
        String sanitized = service.sanitizeString(url);
        assertEquals("Connecting to https://[REDACTED]:[REDACTED]@database.com/db", sanitized);
    }

    @Test
    void sanitizeString_redactsAuthHeaders() {
        String log = "Request failed: Authorization: Bearer eyJhbGciOiJIUzI1Ni... suffix";
        String sanitized = service.sanitizeString(log);
        assertEquals("Request failed: Authorization: Bearer [REDACTED] suffix", sanitized);
    }

    @Test
    void sanitizeString_redactsInlineSecrets() {
        String errorMsg = "Failed to parse token=abc123secret&id=5";
        String sanitized = service.sanitizeString(errorMsg);
        assertEquals("Failed to parse token=[REDACTED]&id=5", sanitized);
    }

    @Test
    void sanitizeMap_handlesNestedStructures() {
        Map<String, Object> nested = new HashMap<>();
        nested.put("client_secret", "secret_value");

        Map<String, Object> payload = new HashMap<>();
        payload.put("config", nested);
        payload.put("history", List.of("safe", Map.of("token", "abc")));

        Map<String, Object> sanitized = service.sanitize(payload);

        @SuppressWarnings("unchecked")
        Map<String, Object> sanitizedNested = (Map<String, Object>) sanitized.get("config");
        assertEquals(DataSanitizationService.REDACTED, sanitizedNested.get("client_secret"));

        @SuppressWarnings("unchecked")
        List<Object> history = (List<Object>) sanitized.get("history");
        assertEquals("safe", history.get(0));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> historyMap = (Map<String, Object>) history.get(1);
        assertEquals(DataSanitizationService.REDACTED, historyMap.get("token"));
    }
}
