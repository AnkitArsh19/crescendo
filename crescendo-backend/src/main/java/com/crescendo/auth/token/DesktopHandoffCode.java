package com.crescendo.auth.token;

import com.crescendo.user.user_command.User_command;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * One-time, short-lived handoff code issued to the browser so the Crescendo desktop
 * app can obtain a real token pair without raw tokens ever appearing in a URL.
 *
 * RFC 8252 pattern (simplified for an already-authenticated browser session):
 *   1. Browser calls POST /auth/desktop-handoff/issue  - receives { code }
 *   2. Browser navigates to /open-app?code=...
 *   3. /open-app fires: crescendo://auth/callback?code=...
 *   4. Desktop app calls POST /auth/desktop-handoff/exchange { code } - { accessToken, refreshToken, ... }
 *
 * Security properties:
 *   Only the SHA-256 hash is stored - raw code never persisted.
 *   Single-use: usedAt is set atomically on first redemption; second attempt is rejected.
 *   60-second TTL: the code is worthless after expiry even if intercepted.
 *   No tokens in browser history: only the short-lived, already-consumed code appears.
 */
@Entity
@Table(
    name = "desktop_handoff_code",
    indexes = {
        @Index(name = "idx_dhc_code_hash",  columnList = "code_hash"),
        @Index(name = "idx_dhc_user_id",    columnList = "user_id"),
        @Index(name = "idx_dhc_expires_at", columnList = "expires_at")
    }
)
public class DesktopHandoffCode {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_desktop_handoff_code_user"))
    private User_command user;

    /** SHA-256 hash of the raw one-time code returned to the browser. */
    @Column(name = "code_hash", nullable = false, length = 100, unique = true)
    private String codeHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** created_at + 60 seconds. The code is rejected after this point. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Set on first successful redemption. A non-null value means the code is used up. */
    @Column(name = "used_at")
    private Instant usedAt;

    /** Optional device ID forwarded from the frontend for session fingerprinting. */
    @Column(name = "device_id", length = 64)
    private String deviceId;

    /** Optional human-readable device label (e.g. "Crescendo Desktop on Windows"). */
    @Column(name = "device_label", length = 100)
    private String deviceLabel;

    public DesktopHandoffCode() {}

    public DesktopHandoffCode(UUID id, User_command user, String codeHash,
                               Instant expiresAt, String deviceId, String deviceLabel) {
        this.id = id;
        this.user = user;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.deviceId = deviceId;
        this.deviceLabel = deviceLabel;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User_command getUser() { return user; }
    public void setUser(User_command user) { this.user = user; }

    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getDeviceLabel() { return deviceLabel; }
    public void setDeviceLabel(String deviceLabel) { this.deviceLabel = deviceLabel; }
}
