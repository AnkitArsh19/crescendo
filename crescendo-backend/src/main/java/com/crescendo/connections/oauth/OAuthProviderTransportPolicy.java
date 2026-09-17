package com.crescendo.connections.oauth;

import java.util.Set;

/**
 * Provider-specific OAuth transport requirements shared by authorization-code
 * exchange and refresh-token exchange.
 *
 * <p>Keeping this policy in one place prevents a provider from working for the
 * initial authorization but failing later when its access token is refreshed.</p>
 */
public final class OAuthProviderTransportPolicy {

    private static final Set<String> BASIC_CLIENT_AUTH_PROVIDERS = Set.of(
            "airtable", "calendly", "figma", "notion", "reddit", "spotify", "twitter", "x"
    );

    private OAuthProviderTransportPolicy() {}

    public static boolean usesBasicClientAuthentication(String providerKey, String clientSecret) {
        return clientSecret != null && !clientSecret.isBlank()
                && BASIC_CLIENT_AUTH_PROVIDERS.contains(providerKey);
    }
}
