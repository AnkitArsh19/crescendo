package com.crescendo.shared.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IpAddressTest {

    @Test
    void validIpv4_shouldSucceed() {
        assertDoesNotThrow(() -> IpAddress.of("192.168.1.1"));
        assertDoesNotThrow(() -> IpAddress.of("0.0.0.0"));
        assertDoesNotThrow(() -> IpAddress.of("255.255.255.255"));
    }

    @Test
    void validIpv6_shouldSucceed() {
        assertDoesNotThrow(() -> IpAddress.of("2001:0db8:85a3:0000:0000:8a2e:0370:7334"));
        assertDoesNotThrow(() -> IpAddress.of("::1"));
        assertDoesNotThrow(() -> IpAddress.of("2001:db8::ff00:42:8329"));
    }

    @Test
    void invalidIp_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> IpAddress.of("256.256.256.256"));
        assertThrows(IllegalArgumentException.class, () -> IpAddress.of("not-an-ip"));
        assertThrows(IllegalArgumentException.class, () -> IpAddress.of("192.168.1"));
        assertThrows(IllegalArgumentException.class, () -> IpAddress.of(""));
        assertThrows(IllegalArgumentException.class, () -> IpAddress.of("   "));
    }

    @Test
    void nullIp_factoryReturnsNull() {
        assertNull(IpAddress.of(null));
    }

    @Test
    void isSuspiciouslyDifferentFrom_detectsDifferences() {
        IpAddress ip1 = IpAddress.of("192.168.1.1");
        IpAddress ip2 = IpAddress.of("192.168.1.1");
        IpAddress ip3 = IpAddress.of("10.0.0.1");

        assertFalse(ip1.isSuspiciouslyDifferentFrom(ip2));
        assertTrue(ip1.isSuspiciouslyDifferentFrom(ip3));
        assertFalse(ip1.isSuspiciouslyDifferentFrom(null));
    }
}
