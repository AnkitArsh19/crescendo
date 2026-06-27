package com.crescendo.publicapi.oauth.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(OAuthAuthorizationConsentId.class)
@Table(name = "oauth2_authorization_consent")
public class OAuthAuthorizationConsentSchema {
    @Id
    @Column(name = "registered_client_id", length = 100)
    private String registeredClientId;

    @Id
    @Column(name = "principal_name", length = 200)
    private String principalName;

    @Column(nullable = false, length = 1000)
    private String authorities;

    protected OAuthAuthorizationConsentSchema() {
    }
}
