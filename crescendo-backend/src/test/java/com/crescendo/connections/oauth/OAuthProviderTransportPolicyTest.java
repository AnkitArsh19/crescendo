package com.crescendo.connections.oauth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuthProviderTransportPolicyTest {

    @Test
    void appliesBasicClientAuthenticationToFigmaAndCalendlyForBothOAuthStages() {
        assertTrue(OAuthProviderTransportPolicy.usesBasicClientAuthentication("figma", "secret"));
        assertTrue(OAuthProviderTransportPolicy.usesBasicClientAuthentication("calendly", "secret"));
    }

    @Test
    void doesNotSendBasicAuthenticationWithoutASecretOrForFormBodyProviders() {
        assertFalse(OAuthProviderTransportPolicy.usesBasicClientAuthentication("figma", ""));
        assertFalse(OAuthProviderTransportPolicy.usesBasicClientAuthentication("typeform", "secret"));
    }
}
