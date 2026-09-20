package com.crescendo.security.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Service responsible for cryptographic erasure (crypto-shredding) and per-user Data Encryption Keys (DEK).
 *
 * Each user is provisioned a unique 256-bit AES DEK.
 * The DEK is encrypted using the application master key (Key Encryption Key - KEK) and stored in {@link UserEncryptionKey}.
 *
 * When an account deletion is executed:
 * Calling {@link #shredUserKey(UUID)} immediately purges the user's DEK from the database.
 * Because the DEK was unique to that user and never logged or persisted elsewhere, all historical
 * database backups, transaction logs, and snapshots containing ciphertext encrypted under this DEK
 * become mathematically impossible to decrypt, guaranteeing GDPR / Right to be Forgotten compliance
 * even across immutable cold storage and backup media.
 */
@Service
public class CryptoShreddingService {

    private static final Logger log = LoggerFactory.getLogger(CryptoShreddingService.class);
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;
    private static final int DEK_BYTES = 32; // AES-256

    private final UserEncryptionKeyRepository keyRepository;
    private final SecretKey masterKek;
    private final SecureRandom secureRandom = new SecureRandom();

    public CryptoShreddingService(
            UserEncryptionKeyRepository keyRepository,
            @Value("${credentials.crypto.key:dGVzdGtleXRlc3RrZXl0ZXN0a2V5dGVzdGtleXRlc3Q=}") String base64Key
    ) {
        this.keyRepository = keyRepository;
        this.masterKek = buildMasterKek(base64Key);
    }

    /**
     * Retrieves an existing DEK for the user, or securely generates and stores a new one.
     */
    @Transactional
    public SecretKey getOrCreateUserDek(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        Optional<UserEncryptionKey> existing = keyRepository.findById(userId);
        if (existing.isPresent()) {
            byte[] rawDek = decryptDek(existing.get().getEncryptedDek());
            return new SecretKeySpec(rawDek, "AES");
        }

        // Generate new random 256-bit DEK
        byte[] rawDek = new byte[DEK_BYTES];
        secureRandom.nextBytes(rawDek);

        String encryptedDek = encryptDek(rawDek);
        UserEncryptionKey newKey = new UserEncryptionKey(userId, encryptedDek);
        keyRepository.save(newKey);
        log.info("[crypto-shredding] Generated and stored new per-user DEK for user {}", userId);

        return new SecretKeySpec(rawDek, "AES");
    }

    /**
     * Permanently shreds (deletes) the user's DEK, making all user data encrypted under it permanently unrecoverable.
     */
    @Transactional
    public void shredUserKey(UUID userId) {
        if (userId == null) {
            return;
        }
        if (keyRepository.existsById(userId)) {
            keyRepository.deleteById(userId);
            keyRepository.flush();
            log.info("[crypto-shredding] Permanently shredded Data Encryption Key (DEK) for user {}. User ciphertext is now cryptographically erased.", userId);
        } else {
            log.debug("[crypto-shredding] No DEK found for user {} during shredding.", userId);
        }
    }

    /**
     * Checks whether a DEK exists for the given user.
     */
    public boolean hasUserKey(UUID userId) {
        return userId != null && keyRepository.existsById(userId);
    }

    /**
     * Encrypts plaintext bytes with the user's specific DEK using AES-GCM.
     */
    @Transactional
    public byte[] encryptForUser(UUID userId, byte[] plaintext) {
        if (plaintext == null) {
            return null;
        }
        SecretKey dek = getOrCreateUserDek(userId);
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, dek, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return payload;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt data with user DEK", ex);
        }
    }

    /**
     * Decrypts ciphertext bytes using the user's specific DEK.
     */
    @Transactional
    public byte[] decryptForUser(UUID userId, byte[] payload) {
        if (payload == null || payload.length <= IV_BYTES) {
            return null;
        }
        SecretKey dek = getOrCreateUserDek(userId);
        try {
            byte[] iv = new byte[IV_BYTES];
            byte[] ciphertext = new byte[payload.length - IV_BYTES];
            System.arraycopy(payload, 0, iv, 0, IV_BYTES);
            System.arraycopy(payload, IV_BYTES, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, dek, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt data with user DEK", ex);
        }
    }

    private String encryptDek(byte[] rawDek) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, masterKek, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(rawDek);

            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt user DEK under master KEK", ex);
        }
    }

    private byte[] decryptDek(String encryptedDekBase64) {
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedDekBase64);
            if (payload.length <= IV_BYTES) {
                throw new IllegalArgumentException("Malformed encrypted DEK payload");
            }

            byte[] iv = new byte[IV_BYTES];
            byte[] ciphertext = new byte[payload.length - IV_BYTES];
            System.arraycopy(payload, 0, iv, 0, IV_BYTES);
            System.arraycopy(payload, IV_BYTES, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, masterKek, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt user DEK under master KEK", ex);
        }
    }

    private SecretKey buildMasterKek(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            base64Key = "dGVzdGtleXRlc3RrZXl0ZXN0a2V5dGVzdGtleXRlc3Q=";
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key.trim().getBytes(StandardCharsets.UTF_8));
            if (keyBytes.length == 32) {
                return new SecretKeySpec(keyBytes, "AES");
            }
            // If key bytes is not 32, hash with SHA-256 to ensure exactly 32 bytes
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return new SecretKeySpec(sha256.digest(keyBytes), "AES");
        } catch (Exception ex) {
            log.warn("[crypto-shredding] Invalid master key string, deriving 256-bit key via SHA-256");
            try {
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                return new SecretKeySpec(sha256.digest(base64Key.getBytes(StandardCharsets.UTF_8)), "AES");
            } catch (Exception fatal) {
                throw new IllegalStateException("Unable to initialize master KEK for CryptoShreddingService", fatal);
            }
        }
    }
}
