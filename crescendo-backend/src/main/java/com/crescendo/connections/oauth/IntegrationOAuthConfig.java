package com.crescendo.connections.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads per-provider OAuth credentials from application properties.
 *
 * <p>Property pattern:
 * <pre>
 *   crescendo.integrations.oauth.providers.{provider-key}.client-id=...
 *   crescendo.integrations.oauth.providers.{provider-key}.client-secret=...
 *   crescendo.integrations.oauth.providers.{provider-key}.authorize-url=...
 *   crescendo.integrations.oauth.providers.{provider-key}.token-url=...
 *   crescendo.integrations.oauth.providers.{provider-key}.scopes=...
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "crescendo.integrations.oauth")
public class IntegrationOAuthConfig {

    private Map<String, ProviderConfig> providers = new HashMap<>();

    public Map<String, ProviderConfig> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, ProviderConfig> providers) {
        this.providers = providers;
    }

    public ProviderConfig getProvider(String providerKey) {
        return providers.get(providerKey);
    }

    public boolean hasProvider(String providerKey) {
        return providers.containsKey(providerKey) && providers.get(providerKey).isConfigured();
    }

    /**
     * Per-provider OAuth configuration values.
     */
    public static class ProviderConfig {

        private String clientId;
        private String clientSecret;
        private String authorizeUrl;
        private String tokenUrl;
        private String scopes;

        /** If true, this provider requires PKCE (Proof Key for Code Exchange). */
        private boolean pkce = false;

        public boolean isConfigured() {
            return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
        }

        // Getters & Setters

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

        public String getAuthorizeUrl() { return authorizeUrl; }
        public void setAuthorizeUrl(String authorizeUrl) { this.authorizeUrl = authorizeUrl; }

        public String getTokenUrl() { return tokenUrl; }
        public void setTokenUrl(String tokenUrl) { this.tokenUrl = tokenUrl; }

        public String getScopes() { return scopes; }
        public void setScopes(String scopes) { this.scopes = scopes; }

        public boolean isPkce() { return pkce; }
        public void setPkce(boolean pkce) { this.pkce = pkce; }
    }
}
