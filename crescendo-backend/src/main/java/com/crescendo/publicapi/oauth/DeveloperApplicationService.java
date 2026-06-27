package com.crescendo.publicapi.oauth;

import com.crescendo.publicapi.PublicApiScopes;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class DeveloperApplicationService {
    private static final int MAX_APPLICATIONS_PER_USER = 20;
    private static final Duration AUTHORIZATION_CODE_TTL = Duration.ofMinutes(5);
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    private final DeveloperApplicationRepository applications;
    private final CrescendoRegisteredClientRepository registeredClients;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final OAuthAccessTokenUsageLogRepository usageLogs;
    private final SecureRandom secureRandom = new SecureRandom();

    public DeveloperApplicationService(
            DeveloperApplicationRepository applications,
            CrescendoRegisteredClientRepository registeredClients,
            BCryptPasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate,
            OAuthAccessTokenUsageLogRepository usageLogs) {
        this.applications = applications;
        this.registeredClients = registeredClients;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.usageLogs = usageLogs;
    }

    @Transactional
    public DeveloperApplicationDto.CreatedResponse create(
            UUID ownerUserId, DeveloperApplicationDto.CreateRequest request) {
        if (applications.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId).size()
                >= MAX_APPLICATIONS_PER_USER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Maximum developer applications reached (" + MAX_APPLICATIONS_PER_USER + ")"
            );
        }

        Set<String> redirectUris = validateRedirectUris(request.redirectUris());
        Set<String> scopes = validateScopes(request.scopes());
        String id = UUID.randomUUID().toString();
        String clientId = "crescendo_" + randomToken(18);
        String plainSecret = request.publicClient() ? null : "cs_live_" + randomToken(32);

        RegisteredClient registeredClient = buildRegisteredClient(
                id,
                clientId,
                request.name(),
                request.publicClient(),
                plainSecret,
                redirectUris,
                scopes
        );
        registeredClients.save(registeredClient);

        DeveloperApplication application = applications.save(new DeveloperApplication(
                id,
                ownerUserId,
                clientId,
                request.name().trim(),
                blankToNull(request.logoUrl()),
                request.publicClient()
        ));
        return new DeveloperApplicationDto.CreatedResponse(
                toResponse(application, registeredClient),
                plainSecret
        );
    }

    public List<DeveloperApplicationDto.ApplicationResponse> list(UUID ownerUserId) {
        return applications.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId).stream()
                .map(application ->
                        toResponse(application, registeredClients.findStoredById(application.getId())))
                .toList();
    }

    public DeveloperApplicationDto.ApplicationResponse get(UUID ownerUserId, String id) {
        DeveloperApplication application = owned(ownerUserId, id);
        return toResponse(application, registeredClients.findStoredById(id));
    }

    public Page<DeveloperApplicationDto.UsageResponse> usage(
            UUID ownerUserId, String id, Pageable pageable) {
        owned(ownerUserId, id);
        return usageLogs.findByApplicationIdOrderByCreatedAtDesc(id, pageable)
                .map(log -> new DeveloperApplicationDto.UsageResponse(
                        log.getId(),
                        log.getAuthorizationId(),
                        log.getUserId(),
                        log.getMethod(),
                        log.getPath(),
                        log.getStatus(),
                        log.getIpAddress(),
                        log.getUserAgent(),
                        log.getCreatedAt()
                ));
    }

    @Transactional
    public DeveloperApplicationDto.ApplicationResponse update(
            UUID ownerUserId, String id, DeveloperApplicationDto.UpdateRequest request) {
        DeveloperApplication application = owned(ownerUserId, id);
        requireActive(application);
        RegisteredClient current = requireRegisteredClient(id);
        Set<String> redirectUris = validateRedirectUris(request.redirectUris());
        Set<String> scopes = validateScopes(request.scopes());

        RegisteredClient updated = RegisteredClient.from(current)
                .clientName(request.name().trim())
                .redirectUris(values -> {
                    values.clear();
                    values.addAll(redirectUris);
                })
                .scopes(values -> {
                    values.clear();
                    values.addAll(scopes);
                })
                .build();
        registeredClients.save(updated);

        application.setName(request.name().trim());
        application.setLogoUrl(blankToNull(request.logoUrl()));
        applications.save(application);
        return toResponse(application, updated);
    }

    @Transactional
    public DeveloperApplicationDto.SecretResponse rotateSecret(UUID ownerUserId, String id) {
        DeveloperApplication application = owned(ownerUserId, id);
        requireActive(application);
        if (application.isPublicClient()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Public PKCE clients do not have a client secret"
            );
        }
        RegisteredClient current = requireRegisteredClient(id);
        String plainSecret = "cs_live_" + randomToken(32);
        RegisteredClient updated = RegisteredClient.from(current)
                .clientSecret(passwordEncoder.encode(plainSecret))
                .clientSecretExpiresAt(null)
                .build();
        registeredClients.save(updated);
        revokeAuthorizations(id);
        return new DeveloperApplicationDto.SecretResponse(plainSecret);
    }

    @Transactional
    public void deactivate(UUID ownerUserId, String id) {
        DeveloperApplication application = owned(ownerUserId, id);
        application.setActive(false);
        applications.save(application);
        revokeAuthorizations(id);
    }

    @Transactional
    public void delete(UUID ownerUserId, String id) {
        DeveloperApplication application = owned(ownerUserId, id);
        revokeAuthorizations(id);
        jdbcTemplate.update(
                "delete from oauth_access_token_usage_log where application_id = ?",
                id
        );
        jdbcTemplate.update("delete from oauth2_registered_client where id = ?", id);
        applications.delete(application);
    }

    private RegisteredClient buildRegisteredClient(
            String id,
            String clientId,
            String name,
            boolean publicClient,
            String plainSecret,
            Set<String> redirectUris,
            Set<String> scopes) {
        RegisteredClient.Builder builder = RegisteredClient.withId(id)
                .clientId(clientId)
                .clientIdIssuedAt(Instant.now())
                .clientName(name.trim())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .clientAuthenticationMethod(publicClient
                        ? ClientAuthenticationMethod.NONE
                        : ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(true)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .authorizationCodeTimeToLive(AUTHORIZATION_CODE_TTL)
                        .accessTokenTimeToLive(ACCESS_TOKEN_TTL)
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                        .reuseRefreshTokens(false)
                        .refreshTokenTimeToLive(REFRESH_TOKEN_TTL)
                        .build());
        if (!publicClient) {
            builder.clientSecret(passwordEncoder.encode(plainSecret));
        }
        redirectUris.forEach(builder::redirectUri);
        scopes.forEach(builder::scope);
        return builder.build();
    }

    private Set<String> validateScopes(List<String> requestedScopes) {
        PublicApiScopes.serialize(requestedScopes);
        return Set.copyOf(requestedScopes);
    }

    private Set<String> validateRedirectUris(List<String> requestedUris) {
        Set<String> uris = Set.copyOf(requestedUris.stream().map(String::trim).toList());
        if (uris.size() != requestedUris.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Redirect URIs must be unique");
        }
        uris.forEach(this::validateRedirectUri);
        return uris;
    }

    private void validateRedirectUri(String value) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            boolean localhost = host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1");
            boolean allowedScheme = scheme.equals("https") || (scheme.equals("http") && localhost);
            if (!uri.isAbsolute() || !allowedScheme || uri.getFragment() != null || uri.getUserInfo() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Redirect URIs must use HTTPS; HTTP is allowed only for localhost"
            );
        }
    }

    private DeveloperApplication owned(UUID ownerUserId, String id) {
        return applications.findByIdAndOwnerUserId(id, ownerUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Developer application not found"
                ));
    }

    private void requireActive(DeveloperApplication application) {
        if (!application.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Developer application is inactive"
            );
        }
    }

    private RegisteredClient requireRegisteredClient(String id) {
        RegisteredClient client = registeredClients.findStoredById(id);
        if (client == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Developer application registration is missing"
            );
        }
        return client;
    }

    private DeveloperApplicationDto.ApplicationResponse toResponse(
            DeveloperApplication application, RegisteredClient client) {
        return new DeveloperApplicationDto.ApplicationResponse(
                application.getId(),
                application.getClientId(),
                application.getName(),
                application.getLogoUrl(),
                application.isPublicClient(),
                application.isActive(),
                application.getRateLimitPerMinute(),
                client == null ? List.of() : List.copyOf(client.getRedirectUris()),
                client == null ? List.of() : List.copyOf(client.getScopes()),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }

    private void revokeAuthorizations(String registeredClientId) {
        jdbcTemplate.update(
                "delete from oauth2_authorization_consent where registered_client_id = ?",
                registeredClientId
        );
        jdbcTemplate.update(
                "delete from oauth2_authorization where registered_client_id = ?",
                registeredClientId
        );
        jdbcTemplate.update(
                "delete from oauth2_consumed_refresh_token where registered_client_id = ?",
                registeredClientId
        );
    }

    private String randomToken(int byteCount) {
        byte[] bytes = new byte[byteCount];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
