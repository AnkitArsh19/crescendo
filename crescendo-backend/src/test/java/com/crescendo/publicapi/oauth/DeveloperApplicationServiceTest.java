package com.crescendo.publicapi.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeveloperApplicationServiceTest {

    @Mock
    private DeveloperApplicationRepository applications;
    @Mock
    private CrescendoRegisteredClientRepository registeredClients;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private OAuthAccessTokenUsageLogRepository usageLogs;

    private DeveloperApplicationService service;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UUID ownerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DeveloperApplicationService(
                applications, registeredClients, passwordEncoder, jdbcTemplate, usageLogs);
    }

    @Test
    void create_validApplication_generatesPkceAndConsentEnabledClient() {
        var req = new DeveloperApplicationDto.CreateRequest(
                "My External Integration",
                "https://myapp.com/logo.png",
                false,
                List.of("https://myapp.com/oauth/callback", "http://localhost:3000/callback"),
                List.of("workflow:read", "workflow:trigger")
        );

        when(applications.findByOwnerUserIdOrderByCreatedAtDesc(ownerId)).thenReturn(List.of());
        when(applications.save(any(DeveloperApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        var res = service.create(ownerId, req);

        assertThat(res.clientSecret()).startsWith("cs_live_");
        assertThat(res.application().name()).isEqualTo("My External Integration");
        assertThat(res.application().clientId()).startsWith("crescendo_");

        ArgumentCaptor<RegisteredClient> captor = ArgumentCaptor.forClass(RegisteredClient.class);
        verify(registeredClients).save(captor.capture());
        RegisteredClient savedClient = captor.getValue();

        // Assert strictly enforced OAuth Authorization Server security policies
        assertThat(savedClient.getClientSettings().isRequireProofKey())
                .as("PKCE must be strictly required for all registered clients")
                .isTrue();
        assertThat(savedClient.getClientSettings().isRequireAuthorizationConsent())
                .as("User consent screen must be strictly enabled for third party developers")
                .isTrue();
        assertThat(savedClient.getTokenSettings().isReuseRefreshTokens())
                .as("Refresh token rotation must be enabled (no reuse)")
                .isFalse();
        assertThat(savedClient.getScopes()).containsExactlyInAnyOrder("workflow:read", "workflow:trigger");
    }

    @Test
    void create_insecureHttpRedirectUri_throwsBadRequest() {
        var req = new DeveloperApplicationDto.CreateRequest(
                "Insecure App",
                null,
                false,
                List.of("http://external-unsecured-site.com/callback"),
                List.of("workflow:read")
        );

        when(applications.findByOwnerUserIdOrderByCreatedAtDesc(ownerId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(ownerId, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Redirect URIs must use HTTPS; HTTP is allowed only for localhost");
    }

    @Test
    void rotateSecret_publicPkceClient_throwsBadRequest() {
        String appId = UUID.randomUUID().toString();
        var publicApp = new DeveloperApplication(appId, ownerId, "crescendo_pub123", "SPA Client", null, true);
        publicApp.setActive(true);

        when(applications.findByIdAndOwnerUserId(appId, ownerId)).thenReturn(Optional.of(publicApp));

        assertThatThrownBy(() -> service.rotateSecret(ownerId, appId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Public PKCE clients do not have a client secret");
    }

    @Test
    void rotateSecret_confidentialClient_rotatesKeyAndRevokesExistingTokens() {
        String appId = UUID.randomUUID().toString();
        var confidentialApp = new DeveloperApplication(appId, ownerId, "crescendo_conf123", "Server App", null, false);
        confidentialApp.setActive(true);
        var originalClient = RegisteredClient.withId(appId)
                .clientId("crescendo_conf123")
                .clientSecret("old_secret_hash")
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://myapp.com/cb")
                .build();

        when(applications.findByIdAndOwnerUserId(appId, ownerId)).thenReturn(Optional.of(confidentialApp));
        when(registeredClients.findStoredById(appId)).thenReturn(originalClient);

        var secretRes = service.rotateSecret(ownerId, appId);

        assertThat(secretRes.clientSecret()).startsWith("cs_live_");

        ArgumentCaptor<RegisteredClient> captor = ArgumentCaptor.forClass(RegisteredClient.class);
        verify(registeredClients).save(captor.capture());
        assertThat(captor.getValue().getClientSecret()).isNotEqualTo("old_secret_hash");

        // Asserts revocation of old access & consent authorizations upon secret rotation
        verify(jdbcTemplate).update("delete from oauth2_authorization where registered_client_id = ?", appId);
        verify(jdbcTemplate).update("delete from oauth2_authorization_consent where registered_client_id = ?", appId);
    }
}
