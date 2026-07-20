package com.crescendo.emailservice.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DomainConnectServiceTest {

    private DomainConnectService service;

    @BeforeEach
    void setUp() {
        service = new DomainConnectService();
        // Point to the real test key in the repo root
        ReflectionTestUtils.setField(service, "privateKeyPath", "../private_key.pem");
        service.init();
    }

    @Test
    void testConstructApplyUrl_SignsProperly() {
        // Arrange
        String urlSyncUx = "https://domainconnect.godaddy.com";
        String tokenValue = "abc123tokenXYZ";
        DomainDto.DnsRecord txtRecord = new DomainDto.DnsRecord("TXT", DnsVerificationService.TXT_RECORD_PREFIX + "test.com", DnsVerificationService.TOKEN_VALUE_PREFIX + tokenValue);
        
        DomainDto.DomainResponse domainDto = new DomainDto.DomainResponse(
                UUID.randomUUID(), "test.com", "PENDING", List.of(txtRecord), null, null, false, false, false,
                50, "WARMING_UP", "UNVERIFIED", "TRANSACTIONAL_ONLY", "PLATFORM", null,
                "GREEN", Collections.emptyList(), true, null, null, null, null, null
        );

        // Act
        String applyUrl = service.constructApplyUrl(urlSyncUx, domainDto);

        // Assert
        assertNotNull(applyUrl);
        assertTrue(applyUrl.startsWith("https://domainconnect.godaddy.com/v2/domainTemplates/providers/crescendo.run/services/email/apply?"));
        assertTrue(applyUrl.contains("domain=test.com"));
        assertTrue(applyUrl.contains("token=" + tokenValue));
        assertTrue(applyUrl.contains("key=key1"));
        assertTrue(applyUrl.contains("sig="));

        // Basic check that sig is URL Encoded (should not contain raw '+', '=', etc unless encoded)
        // Wait, Base64 sig has padding '=', which URL Encoded becomes '%3D'
        // Let's decode the query string and ensure it looks somewhat sane.
        String[] parts = applyUrl.split("\\?");
        assertEquals(2, parts.length);
        String queryString = parts[1];
        
        String decodedQueryString = URLDecoder.decode(queryString, StandardCharsets.UTF_8);
        assertTrue(decodedQueryString.contains("sig="));
    }
    
    @Test
    void testExtractToken_ValidRecord() {
        String tokenValue = "secretToken123";
        DomainDto.DnsRecord txtRecord = new DomainDto.DnsRecord("TXT", "_crescendo-verify.example.com", "crescendo-verify=" + tokenValue);
        DomainDto.DomainResponse domainDto = new DomainDto.DomainResponse(
                UUID.randomUUID(), "example.com", "PENDING", List.of(txtRecord), null, null, false, false, false,
                50, "WARMING_UP", "UNVERIFIED", "TRANSACTIONAL_ONLY", "PLATFORM", null,
                "GREEN", Collections.emptyList(), true, null, null, null, null, null
        );

        String token = service.extractToken(domainDto);
        assertEquals(tokenValue, token);
    }
}
