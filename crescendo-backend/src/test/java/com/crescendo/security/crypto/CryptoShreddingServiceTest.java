package com.crescendo.security.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CryptoShreddingServiceTest {

    @Mock
    private UserEncryptionKeyRepository keyRepository;

    private CryptoShreddingService cryptoShreddingService;
    private final UUID userId = UUID.randomUUID();
    private static final String TEST_KEY_B64 = "dGVzdGtleXRlc3RrZXl0ZXN0a2V5dGVzdGtleXRlc3Q=";

    @BeforeEach
    void setUp() {
        cryptoShreddingService = new CryptoShreddingService(keyRepository, TEST_KEY_B64);
    }

    @Test
    void getOrCreateUserDek_createsNewKeyWhenNoneExists() {
        when(keyRepository.findById(userId)).thenReturn(Optional.empty());

        SecretKey dek = cryptoShreddingService.getOrCreateUserDek(userId);

        assertThat(dek).isNotNull();
        assertThat(dek.getAlgorithm()).isEqualTo("AES");
        assertThat(dek.getEncoded()).hasSize(32); // 256 bits

        ArgumentCaptor<UserEncryptionKey> captor = ArgumentCaptor.forClass(UserEncryptionKey.class);
        verify(keyRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getEncryptedDek()).isNotBlank();
    }

    @Test
    void getOrCreateUserDek_decryptsExistingKey() {
        // First create a key to get the valid encrypted DEK payload
        when(keyRepository.findById(userId)).thenReturn(Optional.empty());
        SecretKey originalDek = cryptoShreddingService.getOrCreateUserDek(userId);

        ArgumentCaptor<UserEncryptionKey> captor = ArgumentCaptor.forClass(UserEncryptionKey.class);
        verify(keyRepository).save(captor.capture());
        String encryptedDek = captor.getValue().getEncryptedDek();

        // Now test retrieval
        when(keyRepository.findById(userId)).thenReturn(Optional.of(new UserEncryptionKey(userId, encryptedDek)));
        SecretKey loadedDek = cryptoShreddingService.getOrCreateUserDek(userId);

        assertThat(loadedDek.getEncoded()).isEqualTo(originalDek.getEncoded());
    }

    @Test
    void shredUserKey_deletesAndFlushesWhenKeyExists() {
        when(keyRepository.existsById(userId)).thenReturn(true);

        cryptoShreddingService.shredUserKey(userId);

        verify(keyRepository).deleteById(userId);
        verify(keyRepository).flush();
    }

    @Test
    void shredUserKey_doesNothingWhenNoKey() {
        when(keyRepository.existsById(userId)).thenReturn(false);

        cryptoShreddingService.shredUserKey(userId);

        verify(keyRepository, never()).deleteById(userId);
    }

    @Test
    void encryptAndDecryptForUser_roundTripsSuccessfully() {
        when(keyRepository.findById(userId)).thenReturn(Optional.empty());

        byte[] plaintext = "super-sensitive-token-or-secret".getBytes(StandardCharsets.UTF_8);
        byte[] ciphertext = cryptoShreddingService.encryptForUser(userId, plaintext);

        assertThat(ciphertext).isNotNull();
        assertThat(ciphertext).isNotEqualTo(plaintext);

        // For decrypt, mock keyRepository to return the saved key
        ArgumentCaptor<UserEncryptionKey> captor = ArgumentCaptor.forClass(UserEncryptionKey.class);
        verify(keyRepository).save(captor.capture());
        when(keyRepository.findById(userId)).thenReturn(Optional.of(captor.getValue()));

        byte[] decrypted = cryptoShreddingService.decryptForUser(userId, ciphertext);
        assertThat(new String(decrypted, StandardCharsets.UTF_8)).isEqualTo("super-sensitive-token-or-secret");
    }
}
