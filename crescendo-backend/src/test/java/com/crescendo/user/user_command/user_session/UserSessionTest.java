package com.crescendo.user.user_command.user_session;

import com.crescendo.shared.domain.valueobject.DeviceId;
import com.crescendo.shared.domain.valueobject.IpAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserSessionTest {

    @Test
    @DisplayName("rotateToken preserves session identity and updates token hash, expiry, and predecessor")
    void rotateToken_preservesSessionIdentity() {
        UUID sessionId = UUID.randomUUID();
        Instant initialExpiry = Instant.now().plusSeconds(3600);
        UserSession session = new UserSession(sessionId, null, "initialHash123", initialExpiry);

        session.applyFingerprint("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0", "192.168.1.1", "device-xyz", null);
        Instant createdAt = Instant.now().minusSeconds(60);
        session.setCreatedAt(createdAt);

        String newHash = "newRotatedHash456";
        Instant newExpiry = Instant.now().plusSeconds(7200);
        String predecessorHash = "initialHash123";

        session.rotateToken(newHash, newExpiry, predecessorHash);

        // Verify identity preservation
        assertThat(session.getId()).isEqualTo(sessionId);
        assertThat(session.getCreatedAt()).isEqualTo(createdAt);
        assertThat(session.getDeviceId()).isEqualTo(DeviceId.of("device-xyz"));
        assertThat(session.getClientIp()).isEqualTo(IpAddress.of("192.168.1.1"));
        assertThat(session.getDeviceLabel()).isEqualTo("Chrome on Windows");

        // Verify rotated fields
        assertThat(session.getRefreshTokenHash()).isEqualTo(newHash);
        assertThat(session.getExpiresAt()).isEqualTo(newExpiry);
        assertThat(session.getPredecessorTokenHash()).isEqualTo(predecessorHash);
        assertThat(session.getLastUsedAt()).isNotNull();
        assertThat(session.getRevokedAt()).isNull();
    }

    @Test
    @DisplayName("applyFingerprint parses user agent into human-readable label")
    void applyFingerprint_parsesDeviceLabel() {
        UserSession session = new UserSession(UUID.randomUUID(), null, "hash", Instant.now().plusSeconds(3600));

        session.applyFingerprint("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15", "10.0.0.1", "mac-1", null);

        assertThat(session.getDeviceLabel()).isEqualTo("Safari on macOS");
        assertThat(session.getClientIp()).isEqualTo(IpAddress.of("10.0.0.1"));
        assertThat(session.getLastIp()).isEqualTo(IpAddress.of("10.0.0.1"));
    }
}
