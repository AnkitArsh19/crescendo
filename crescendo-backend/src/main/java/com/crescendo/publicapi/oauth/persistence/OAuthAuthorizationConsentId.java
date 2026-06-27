package com.crescendo.publicapi.oauth.persistence;

import java.io.Serializable;
import java.util.Objects;

public class OAuthAuthorizationConsentId implements Serializable {
    private String registeredClientId;
    private String principalName;

    public OAuthAuthorizationConsentId() {
    }

    public OAuthAuthorizationConsentId(String registeredClientId, String principalName) {
        this.registeredClientId = registeredClientId;
        this.principalName = principalName;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof OAuthAuthorizationConsentId that)) return false;
        return Objects.equals(registeredClientId, that.registeredClientId)
                && Objects.equals(principalName, that.principalName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registeredClientId, principalName);
    }
}
