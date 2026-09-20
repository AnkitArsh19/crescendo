package com.crescendo.security.crypto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Stores a per-user 256-bit Data Encryption Key (DEK).
 * The DEK itself is encrypted under the application master key (Key Encryption Key - KEK).
 *
 * Cryptographic Erasure (Crypto-Shredding):
 * When a user requests account deletion, this row is permanently destroyed.
 * Any historical database backups, WAL archives, or cold replicas containing
 * the user's encrypted credentials become mathematically impossible to decrypt.
 */
@Entity
@Table(name = "user_encryption_key")
public class UserEncryptionKey {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "encrypted_dek", nullable = false, length = 512)
    private String encryptedDek;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserEncryptionKey() {}

    public UserEncryptionKey(UUID userId, String encryptedDek) {
        this.userId = userId;
        this.encryptedDek = encryptedDek;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEncryptedDek() {
        return encryptedDek;
    }

    public void setEncryptedDek(String encryptedDek) {
        this.encryptedDek = encryptedDek;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
