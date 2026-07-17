package com.crescendo.user.user_command.webauthn;

import com.crescendo.user.user_command.User_command;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "passkey_credential_command",
        indexes = {
                @Index(name = "idx_passkey_credential_user_id", columnList = "user_id")
        }
)
public class PasskeyCredential_command {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, foreignKey = @ForeignKey(name = "fk_passkey_user"))
    private User_command user;

    // Credential IDs may be up to 1023 bytes. A bounded binary column remains
    // indexable on both PostgreSQL and MySQL.
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "credential_id", nullable = false, unique = true, length = 1024)
    private byte[] credentialId;

    // Avoid PostgreSQL-specific `bytea` column definitions: this service also
    // supports MySQL.
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(name = "public_key", nullable = false)
    private byte[] publicKey;

    /** Serialized WebAuthn4J AttestedCredentialData (AAGUID, credential ID and COSE key). */
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(name = "attested_credential_data")
    private byte[] attestedCredentialData;

    @Column(name = "sign_count", nullable = false)
    private long signCount = 0;

    @Column(name = "transports", length = 255)
    private String transports;

    @Column(name = "is_backed_up", nullable = false)
    private boolean isBackedUp;

    @Column(name = "is_backup_eligible", nullable = false, columnDefinition = "boolean default false")
    private boolean backupEligible;

    @Column(name = "uv_initialized", nullable = false, columnDefinition = "boolean default false")
    private boolean userVerificationInitialized;

    @Column(name = "credential_name", length = 100)
    private String credentialName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    public PasskeyCredential_command() {
    }

    public PasskeyCredential_command(UUID id, User_command user, byte[] credentialId, byte[] publicKey,
                                     byte[] attestedCredentialData, long signCount, String transports,
                                     boolean backupEligible, boolean isBackedUp,
                                     boolean userVerificationInitialized, String credentialName) {
        this.id = id;
        this.user = user;
        this.credentialId = credentialId;
        this.publicKey = publicKey;
        this.attestedCredentialData = attestedCredentialData;
        this.signCount = signCount;
        this.transports = transports;
        this.backupEligible = backupEligible;
        this.isBackedUp = isBackedUp;
        this.userVerificationInitialized = userVerificationInitialized;
        this.credentialName = credentialName;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public User_command getUser() { return user; }
    public void setUser(User_command user) { this.user = user; }
    
    public byte[] getCredentialId() { return credentialId; }
    public void setCredentialId(byte[] credentialId) { this.credentialId = credentialId; }
    
    public byte[] getPublicKey() { return publicKey; }
    public void setPublicKey(byte[] publicKey) { this.publicKey = publicKey; }

    public byte[] getAttestedCredentialData() { return attestedCredentialData; }
    public void setAttestedCredentialData(byte[] attestedCredentialData) { this.attestedCredentialData = attestedCredentialData; }
    
    public long getSignCount() { return signCount; }
    public void setSignCount(long signCount) { this.signCount = signCount; }
    
    public String getTransports() { return transports; }
    public void setTransports(String transports) { this.transports = transports; }
    
    public boolean isBackedUp() { return isBackedUp; }
    public void setBackedUp(boolean backedUp) { isBackedUp = backedUp; }

    public boolean isBackupEligible() { return backupEligible; }
    public void setBackupEligible(boolean backupEligible) { this.backupEligible = backupEligible; }

    public boolean isUserVerificationInitialized() { return userVerificationInitialized; }
    public void setUserVerificationInitialized(boolean userVerificationInitialized) { this.userVerificationInitialized = userVerificationInitialized; }

    public String getCredentialName() { return credentialName; }
    public void setCredentialName(String credentialName) { this.credentialName = credentialName; }
    
    public Instant getCreatedAt() { return createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    
    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}
