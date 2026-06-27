package com.crescendo.publicapi.oauth.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Schema mapping for Spring Authorization Server's JdbcRegisteredClientRepository.
 * Protocol data is read and written through JDBC; this entity lets Hibernate manage the table.
 */
@Entity
@Table(name = "oauth2_registered_client")
public class OAuthRegisteredClientSchema {
    @Id
    @Column(length = 100)
    private String id;

    @Column(name = "client_id", nullable = false, length = 100)
    private String clientId;

    @Column(name = "client_id_issued_at", nullable = false, columnDefinition = "timestamptz")
    private Instant clientIdIssuedAt;

    @Column(name = "client_secret", length = 200)
    private String clientSecret;

    @Column(name = "client_secret_expires_at", columnDefinition = "timestamptz")
    private Instant clientSecretExpiresAt;

    @Column(name = "client_name", nullable = false, length = 200)
    private String clientName;

    @Column(name = "client_authentication_methods", nullable = false, length = 1000)
    private String clientAuthenticationMethods;

    @Column(name = "authorization_grant_types", nullable = false, length = 1000)
    private String authorizationGrantTypes;

    @Column(name = "redirect_uris", length = 1000)
    private String redirectUris;

    @Column(name = "post_logout_redirect_uris", length = 1000)
    private String postLogoutRedirectUris;

    @Column(nullable = false, length = 1000)
    private String scopes;

    @Column(name = "client_settings", nullable = false, length = 2000)
    private String clientSettings;

    @Column(name = "token_settings", nullable = false, length = 2000)
    private String tokenSettings;

    protected OAuthRegisteredClientSchema() {
    }
}
