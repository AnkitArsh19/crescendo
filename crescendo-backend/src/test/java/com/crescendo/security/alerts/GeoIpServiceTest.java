package com.crescendo.security.alerts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GeoIpServiceTest {

    private GeoIpService geoIpService;

    @BeforeEach
    void setUp() {
        // Disabled auto-update and dummy path so it doesn't attempt network downloads during unit tests
        geoIpService = new GeoIpService("", "", false);
    }

    @Test
    @DisplayName("Loopback IPv4 (127.0.0.1) resolves to Localhost representation")
    void testLoopbackIPv4() {
        Optional<GeoIpService.GeoLocation> loc = geoIpService.lookupLocation("127.0.0.1");
        assertTrue(loc.isPresent());
        assertEquals("Localhost", loc.get().city());
        assertEquals("Local Network", loc.get().country());
        assertEquals("Localhost, Local Network", loc.get().toDisplayString());
        // Loopback should return empty for country code to avoid triggering country-anomaly alerts
        assertTrue(geoIpService.lookupCountry("127.0.0.1").isEmpty());
    }

    @Test
    @DisplayName("Site-local private IPs (192.168.x.x, 10.x.x.x) resolve to Local Network")
    void testSiteLocalIP() {
        Optional<GeoIpService.GeoLocation> loc = geoIpService.lookupLocation("192.168.1.50");
        assertTrue(loc.isPresent());
        assertEquals("Localhost, Local Network", loc.get().toDisplayString());
    }

    @Test
    @DisplayName("Null or blank IP returns Optional.empty()")
    void testNullOrBlank() {
        assertTrue(geoIpService.lookupLocation(null).isEmpty());
        assertTrue(geoIpService.lookupLocation("").isEmpty());
        assertTrue(geoIpService.lookupLocation("   ").isEmpty());
    }

    @Test
    @DisplayName("GeoLocation toDisplayString formats correctly with different combinations")
    void testDisplayStringCombinations() {
        GeoIpService.GeoLocation full = new GeoIpService.GeoLocation("San Francisco", "California", "United States", "US");
        assertEquals("San Francisco, California, United States", full.toDisplayString());

        GeoIpService.GeoLocation noRegion = new GeoIpService.GeoLocation("Singapore", null, "Singapore", "SG");
        assertEquals("Singapore, Singapore", noRegion.toDisplayString());

        GeoIpService.GeoLocation countryOnly = new GeoIpService.GeoLocation(null, null, "India", "IN");
        assertEquals("India", countryOnly.toDisplayString());
    }
}
