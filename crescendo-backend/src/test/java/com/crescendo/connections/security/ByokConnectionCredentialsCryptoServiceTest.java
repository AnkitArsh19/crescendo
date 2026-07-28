package com.crescendo.connections.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ByokConnectionCredentialsCryptoServiceTest {

    private ConnectionCredentialsCryptoService cryptoService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    // 32-byte test key (AES-256) encoded in Base64: "0123456789abcdef0123456789abcdef"
    private final String testKeyBase64 = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    @BeforeEach
    void setUp() {
        cryptoService = new ConnectionCredentialsCryptoService(objectMapper, testKeyBase64);
    }

    @Test
    void seal_normalApiKey_encryptsPlaintextWithAesGcmEnvelope() {
        Map<String, Object> plainCredentials = Map.of(
                "apiKey", "sk-proj-super-secret-api-key-12345",
                "orgId", "org-98765"
        );

        Map<String, Object> sealed = cryptoService.seal(plainCredentials);

        // Verify no plaintext API key exists in the sealed map
        assertThat(sealed.get("apiKey")).isNotEqualTo("sk-proj-super-secret-api-key-12345");
        assertThat(sealed.get("apiKey")).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> apiKeyEnvelope = (Map<String, Object>) sealed.get("apiKey");
        assertThat(apiKeyEnvelope).containsKey("__enc");
        assertThat(apiKeyEnvelope.get("__enc").toString()).startsWith("v1:");
    }

    @Test
    void open_sealedCredentials_decryptsBackToOriginalPlaintext() {
        Map<String, Object> original = Map.of(
                "apiKey", "sk-ant-api03-anthropic-byok-secret",
                "environment", "production"
        );

        Map<String, Object> sealed = cryptoService.seal(original);
        Map<String, Object> opened = cryptoService.open(sealed);

        assertThat(opened).containsEntry("apiKey", "sk-ant-api03-anthropic-byok-secret");
        assertThat(opened).containsEntry("environment", "production");
    }

    @Test
    void sealAndOpen_nestedCredentialsAndLists_handlesComplexByokPayloads() {
        Map<String, Object> complex = Map.of(
                "tokens", List.of("token-a", "token-b"),
                "config", Map.of("webhookSecret", "whsec_byok_signature")
        );

        Map<String, Object> sealed = cryptoService.seal(complex);
        Map<String, Object> opened = cryptoService.open(sealed);

        @SuppressWarnings("unchecked")
        List<String> tokens = (List<String>) opened.get("tokens");
        assertThat(tokens).containsExactly("token-a", "token-b");

        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) opened.get("config");
        assertThat(config).containsEntry("webhookSecret", "whsec_byok_signature");
    }

    @Test
    void open_legacyPlaintextCredentials_returnsPlaintextSafelyWithoutCrashing() {
        Map<String, Object> legacy = Map.of(
                "apiKey", "legacy-plaintext-key"
        );

        Map<String, Object> opened = cryptoService.open(legacy);

        assertThat(opened).containsEntry("apiKey", "legacy-plaintext-key");
    }

    @Test
    void constructor_invalidKeyLength_throwsException() {
        String shortKey = Base64.getEncoder().encodeToString("too-short-key".getBytes());
        assertThatThrownBy(() -> new ConnectionCredentialsCryptoService(objectMapper, shortKey))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must decode to exactly 32 bytes");
    }
}
