package com.crescendo.shared.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceIdTest {

    @Test
    void validDeviceId_shouldSucceed() {
        assertDoesNotThrow(() -> DeviceId.of("device-123-abc"));
        assertDoesNotThrow(() -> DeviceId.of("A987F98-A87A-1287-98789A98F"));
        
        DeviceId deviceId = DeviceId.of("   my-device   ");
        assertEquals("my-device", deviceId.value());
    }

    @Test
    void invalidDeviceId_shouldThrowException() {
        // Exceeds 128 characters
        String tooLong = "a".repeat(129);
        assertThrows(IllegalArgumentException.class, () -> DeviceId.of(tooLong));
    }

    @Test
    void blankDeviceId_factoryReturnsNull() {
        assertNull(DeviceId.of(null));
        assertNull(DeviceId.of(""));
        assertNull(DeviceId.of("   "));
    }
}
