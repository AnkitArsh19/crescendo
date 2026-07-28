package com.crescendo.emailservice.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DnsVerificationServiceTest {

    @Test
    @DisplayName("verifyDomainTxtRecord returns true when matching crescendo-verify token is found")
    void verifyDomainTxtRecord_success() {
        var service = new TestableDnsVerificationService(Map.of(
                "_crescendo-verify.example.com", List.of("\"crescendo-verify=token-12345\"")
        ));

        boolean result = service.verifyDomainTxtRecord("example.com", List.of("token-12345"));
        assertTrue(result, "Domain TXT record should be verified when token matches");
    }

    @Test
    @DisplayName("verifyDomainTxtRecord returns false when token does not match")
    void verifyDomainTxtRecord_mismatch() {
        var service = new TestableDnsVerificationService(Map.of(
                "_crescendo-verify.example.com", List.of("\"crescendo-verify=wrong-token\"")
        ));

        boolean result = service.verifyDomainTxtRecord("example.com", List.of("token-12345"));
        assertFalse(result, "Verification should fail when token value does not match");
    }

    @Test
    @DisplayName("verifySpf returns true when SPF record includes spf.crescendo.run")
    void verifySpf_success() {
        var service = new TestableDnsVerificationService(Map.of(
                "mail.example.com", List.of("\"v=spf1 include:spf.crescendo.run ~all\"")
        ));

        boolean result = service.verifySpf("mail.example.com");
        assertTrue(result, "SPF verification should succeed when include is present");
    }

    @Test
    @DisplayName("verifyDkim returns true when DKIM1 header record is found")
    void verifyDkim_success() {
        var service = new TestableDnsVerificationService(Map.of(
                "crescendo._domainkey.mail.example.com", List.of("\"v=DKIM1; k=rsa; p=MIGfMA0GCS...\"")
        ));

        boolean result = service.verifyDkim("crescendo", "mail.example.com");
        assertTrue(result, "DKIM verification should succeed when v=DKIM1 record exists");
    }

    @Test
    @DisplayName("verifyDmarc returns true when DMARC1 record is found")
    void verifyDmarc_success() {
        var service = new TestableDnsVerificationService(Map.of(
                "_dmarc.mail.example.com", List.of("\"v=DMARC1; p=reject; sp=reject\"")
        ));

        boolean result = service.verifyDmarc("mail.example.com");
        assertTrue(result, "DMARC verification should succeed when v=DMARC1 record exists");
    }

    private static class TestableDnsVerificationService extends DnsVerificationService {
        private final Map<String, List<String>> fixtures;

        TestableDnsVerificationService(Map<String, List<String>> fixtures) {
            this.fixtures = fixtures;
        }

        @Override
        List<String> lookupTxtRecords(String hostname) {
            return fixtures.getOrDefault(hostname, List.of());
        }
    }
}
