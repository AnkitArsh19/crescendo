package com.crescendo.connections.security;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
public class ConnectionCredentialsCryptoService {

    private static final String ENC_MARKER = "__enc";
    private static final String ENVELOPE_PREFIX = "v1:";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;

    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKey secretKey;

    public ConnectionCredentialsCryptoService(
            ObjectMapper objectMapper,
            @Value("${credentials.crypto.key:}") String base64Key
    ) {
        this.objectMapper = objectMapper;
        this.secretKey = buildSecretKey(base64Key);
    }

    public Map<String, Object> seal(Map<String, Object> credentials) {
        if (credentials == null) return Map.of();
        return transformMap(credentials, true);
    }

    public Map<String, Object> open(Map<String, Object> credentials) {
        if (credentials == null) return Map.of();
        return transformMap(credentials, false);
    }

    private Map<String, Object> transformMap(Map<String, Object> input, boolean encrypt) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : input.entrySet()) {
            out.put(e.getKey(), transformValue(e.getValue(), encrypt));
        }
        return out;
    }

    private Object transformValue(Object value, boolean encrypt) {
        if (value == null) return null;

        if (encrypt) {
            if (value instanceof Map<?, ?> mapValue) {
                @SuppressWarnings("unchecked")
                Map<String, Object> castMap = (Map<String, Object>) mapValue;
                return transformMap(castMap, true);
            }
            if (value instanceof List<?> listValue) {
                List<Object> out = new ArrayList<>(listValue.size());
                for (Object item : listValue) out.add(transformValue(item, true));
                return out;
            }
            return Map.of(ENC_MARKER, encryptAny(value));
        }

        if (value instanceof Map<?, ?> mapValue) {
            Object marker = mapValue.get(ENC_MARKER);
            if (marker instanceof String markerStr && markerStr.startsWith(ENVELOPE_PREFIX)) {
                return decryptAny(markerStr);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> castMap = (Map<String, Object>) mapValue;
            return transformMap(castMap, false);
        }

        if (value instanceof List<?> listValue) {
            List<Object> out = new ArrayList<>(listValue.size());
            for (Object item : listValue) out.add(transformValue(item, false));
            return out;
        }

        // Legacy plaintext data remains readable so existing connections do not break.
        return value;
    }

    private String encryptAny(Object value) {
        try {
            byte[] plaintext = objectMapper.writeValueAsBytes(value);
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return ENVELOPE_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to encrypt credentials");
        }
    }

    private Object decryptAny(String envelope) {
        try {
            String b64 = envelope.substring(ENVELOPE_PREFIX.length());
            byte[] payload = Base64.getDecoder().decode(b64);
            if (payload.length <= IV_BYTES) {
                throw new IllegalArgumentException("Malformed encrypted payload");
            }

            byte[] iv = new byte[IV_BYTES];
            byte[] ciphertext = new byte[payload.length - IV_BYTES];
            System.arraycopy(payload, 0, iv, 0, IV_BYTES);
            System.arraycopy(payload, IV_BYTES, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);

            return objectMapper.readValue(plaintext, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to decrypt credentials");
        }
    }

    private SecretKey buildSecretKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("credentials.crypto.key must be configured");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key.trim().getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("credentials.crypto.key must be a valid Base64 string");
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException("credentials.crypto.key must decode to exactly 32 bytes (AES-256)");
        }
        return new SecretKeySpec(keyBytes, "AES");
    }
}
