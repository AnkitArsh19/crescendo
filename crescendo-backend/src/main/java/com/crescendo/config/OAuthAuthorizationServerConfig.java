package com.crescendo.config;

import com.crescendo.publicapi.oauth.CrescendoRegisteredClientRepository;
import com.crescendo.publicapi.oauth.DeveloperApplicationRepository;
import com.crescendo.security.OAuthRefreshTokenReuseDetectionFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;


@Configuration
public class OAuthAuthorizationServerConfig {

    @Bean
    public CrescendoRegisteredClientRepository registeredClientRepository(
            JdbcTemplate jdbcTemplate,
            DeveloperApplicationRepository applications) {
        return new CrescendoRegisteredClientRepository(
                new JdbcRegisteredClientRepository(jdbcTemplate),
                applications
        );
    }

    @Bean
    public OAuth2AuthorizationService oauth2AuthorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClients) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClients);
    }

    @Bean
    public OAuth2AuthorizationConsentService oauth2AuthorizationConsentService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClients) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClients);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(
            @Value("${app.public-api.issuer:http://localhost:8080}") String issuer) {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            HttpSessionSecurityContextRepository sessionSecurityContextRepository,
            OAuthRefreshTokenReuseDetectionFilter refreshTokenReuseDetectionFilter)
            throws Exception {
        AuthenticationEntryPoint loginEntryPoint =
                new LoginUrlAuthenticationEntryPoint("/oauth/session-required");

        http.oauth2AuthorizationServer(authorizationServer ->
                http.securityMatcher(authorizationServer.getEndpointsMatcher()));
        http
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .securityContext(context ->
                        context.securityContextRepository(sessionSecurityContextRepository))
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(loginEntryPoint))
                .addFilterBefore(
                        refreshTokenReuseDetectionFilter,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    public HttpSessionSecurityContextRepository oauthSessionSecurityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
