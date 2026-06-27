package com.crescendo.publicapi.oauth.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * PostgreSQL-safe schema mapping for JdbcOAuth2AuthorizationService.
 */
@Entity
@Table(name = "oauth2_authorization")
public class OAuthAuthorizationSchema {
    @Id
    @Column(length = 100)
    private String id;

    @Column(name = "registered_client_id", nullable = false, length = 100)
    private String registeredClientId;

    @Column(name = "principal_name", nullable = false, length = 200)
    private String principalName;

    @Column(name = "authorization_grant_type", nullable = false, length = 100)
    private String authorizationGrantType;

    @Column(name = "authorized_scopes", length = 1000)
    private String authorizedScopes;

    @Column(columnDefinition = "TEXT")
    private String attributes;

    @Column(length = 500)
    private String state;

    @Column(name = "authorization_code_value", columnDefinition = "TEXT")
    private String authorizationCodeValue;
    @Column(name = "authorization_code_issued_at", columnDefinition = "timestamptz")
    private Instant authorizationCodeIssuedAt;
    @Column(name = "authorization_code_expires_at", columnDefinition = "timestamptz")
    private Instant authorizationCodeExpiresAt;
    @Column(name = "authorization_code_metadata", columnDefinition = "TEXT")
    private String authorizationCodeMetadata;

    @Column(name = "access_token_value", columnDefinition = "TEXT")
    private String accessTokenValue;
    @Column(name = "access_token_issued_at", columnDefinition = "timestamptz")
    private Instant accessTokenIssuedAt;
    @Column(name = "access_token_expires_at", columnDefinition = "timestamptz")
    private Instant accessTokenExpiresAt;
    @Column(name = "access_token_metadata", columnDefinition = "TEXT")
    private String accessTokenMetadata;
    @Column(name = "access_token_type", length = 100)
    private String accessTokenType;
    @Column(name = "access_token_scopes", length = 1000)
    private String accessTokenScopes;

    @Column(name = "oidc_id_token_value", columnDefinition = "TEXT")
    private String oidcIdTokenValue;
    @Column(name = "oidc_id_token_issued_at", columnDefinition = "timestamptz")
    private Instant oidcIdTokenIssuedAt;
    @Column(name = "oidc_id_token_expires_at", columnDefinition = "timestamptz")
    private Instant oidcIdTokenExpiresAt;
    @Column(name = "oidc_id_token_metadata", columnDefinition = "TEXT")
    private String oidcIdTokenMetadata;

    @Column(name = "refresh_token_value", columnDefinition = "TEXT")
    private String refreshTokenValue;
    @Column(name = "refresh_token_issued_at", columnDefinition = "timestamptz")
    private Instant refreshTokenIssuedAt;
    @Column(name = "refresh_token_expires_at", columnDefinition = "timestamptz")
    private Instant refreshTokenExpiresAt;
    @Column(name = "refresh_token_metadata", columnDefinition = "TEXT")
    private String refreshTokenMetadata;

    @Column(name = "user_code_value", columnDefinition = "TEXT")
    private String userCodeValue;
    @Column(name = "user_code_issued_at", columnDefinition = "timestamptz")
    private Instant userCodeIssuedAt;
    @Column(name = "user_code_expires_at", columnDefinition = "timestamptz")
    private Instant userCodeExpiresAt;
    @Column(name = "user_code_metadata", columnDefinition = "TEXT")
    private String userCodeMetadata;

    @Column(name = "device_code_value", columnDefinition = "TEXT")
    private String deviceCodeValue;
    @Column(name = "device_code_issued_at", columnDefinition = "timestamptz")
    private Instant deviceCodeIssuedAt;
    @Column(name = "device_code_expires_at", columnDefinition = "timestamptz")
    private Instant deviceCodeExpiresAt;
    @Column(name = "device_code_metadata", columnDefinition = "TEXT")
    private String deviceCodeMetadata;

    protected OAuthAuthorizationSchema() {
    }
}
