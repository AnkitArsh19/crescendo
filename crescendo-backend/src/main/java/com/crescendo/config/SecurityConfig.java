package com.crescendo.config;

import com.crescendo.security.ApiKeyAuthenticationFilter;
import com.crescendo.security.AuthRateLimitingFilter;
import com.crescendo.security.JWTFilter;
import com.crescendo.security.OAuthAccessTokenAuthenticationFilter;
import com.crescendo.security.error.JsonAuthenticationEntryPoint;
import com.crescendo.security.error.JsonAccessDeniedHandler;
import com.crescendo.security.oauth.OAuth2LoginSuccessHandler;
import com.crescendo.security.oauth.OAuth2LoginFailureHandler;
import com.crescendo.security.oauth.CookieOAuth2AuthorizationRequestRepository;
import com.crescendo.security.oauth.GitHubEmailOAuth2UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JWTFilter jwtFilter;
    private final ApiKeyAuthenticationFilter apiKeyFilter;
    private final OAuthAccessTokenAuthenticationFilter oauthAccessTokenFilter;
    private final AuthRateLimitingFilter authRateLimitingFilter;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;
    private final OAuth2LoginSuccessHandler oAuth2SuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2FailureHandler;
    private final CookieOAuth2AuthorizationRequestRepository cookieAuthzRequestRepo;
    private final GitHubEmailOAuth2UserService gitHubEmailOAuth2UserService;

    @Value("${app.security.cors.allowed-origins:}")
    private List<String> allowedOrigins;

    public SecurityConfig(JWTFilter jwtFilter,
            ApiKeyAuthenticationFilter apiKeyFilter,
            OAuthAccessTokenAuthenticationFilter oauthAccessTokenFilter,
            AuthRateLimitingFilter authRateLimitingFilter,
            JsonAuthenticationEntryPoint authenticationEntryPoint,
            JsonAccessDeniedHandler accessDeniedHandler,
            OAuth2LoginSuccessHandler oAuth2SuccessHandler,
            OAuth2LoginFailureHandler oAuth2FailureHandler,
            CookieOAuth2AuthorizationRequestRepository cookieAuthzRequestRepo,
            GitHubEmailOAuth2UserService gitHubEmailOAuth2UserService) {
        this.jwtFilter = jwtFilter;
        this.apiKeyFilter = apiKeyFilter;
        this.oauthAccessTokenFilter = oauthAccessTokenFilter;
        this.authRateLimitingFilter = authRateLimitingFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.oAuth2FailureHandler = oAuth2FailureHandler;
        this.cookieAuthzRequestRepo = cookieAuthzRequestRepo;
        this.gitHubEmailOAuth2UserService = gitHubEmailOAuth2UserService;
    }

    /**
     * Defines the entire HTTP security policy for the application.
     * Configures CORS, CSRF, session management, security headers, endpoint access
     * rules,
     * error handling, and inserts the JWT filter before Spring's default
     * username/password filter.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        /// CSRF is disabled because this app uses stateless JWT authentication. The
        /// refresh token cookie has SameSite=Strict which already prevents cross-site
        /// request forgery at the browser level. Keeping CSRF enabled with Spring
        /// Security 6.x introduces XOR-masked token handling that breaks standard axios
        /// CSRF cookie flow.
        httpSecurity
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                // STATELESS: no server-side HttpSession created. Each request must carry its
                // own JWT.
                // OAuth2 temporarily needs a session to store the authorization request between
                // redirect
                // and callback — Spring handles this internally, and the session is discarded
                // after.
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'none'; frame-ancestors 'none'; form-action 'self'"))
                        .xssProtection(Customizer.withDefaults())
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/login",
                                "/auth/register",
                                "/auth/refresh",
                                "/auth/forgot-password",
                                "/auth/reset-password",
                                "/auth/verify-email", // token is in query param, no auth header available
                                "/mfa/challenge", // called before tokens are issued (post-login MFA step)
                                "/mfa/backup-code", // called before tokens are issued (backup code login)
                                "/actuator/health",
                                "/oauth/session-required")
                        .permitAll()
                        .requestMatchers("/guest/**").permitAll() // guest workflows — identified by X-Guest-Session
                                                                  // header
                        .requestMatchers(HttpMethod.POST, "/webhooks/**").permitAll() // external webhook ingestion
                        .requestMatchers(HttpMethod.GET, "/webhooks/email-events").permitAll() // provider delivery
                                                                                               // callbacks
                        .requestMatchers("/public/forms/**", "/public/approvals/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/t/**").permitAll() // open/click tracking pixel & redirect
                        .requestMatchers(HttpMethod.GET, "/unsubscribe").permitAll() // one-click unsubscribe page
                        .requestMatchers("/oauth2/authorization/**", "/login/oauth2/code/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/connections/oauth/*/callback").permitAll() // integration
                                                                                                      // OAuth callback
                                                                                                      // (state carries
                                                                                                      // userId)
                        .requestMatchers(HttpMethod.GET, "/shared/**").permitAll() // public shared workflow previews
                        .requestMatchers(HttpMethod.GET, "/admin/platform-keys/available").permitAll() // public: which
                                                                                                       // apps have
                                                                                                       // platform keys
                        .requestMatchers(HttpMethod.GET, "/internal/catalog/**").permitAll() // AI microservice catalog polling
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                // OAuth2 login: Spring handles the redirect to Google/GitHub and the callback.
                // Our custom handlers take over after the provider returns.
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(auth -> auth
                                .baseUri("/oauth2/authorization") // default, but explicit for clarity
                                .authorizationRequestRepository(cookieAuthzRequestRepo))
                        .userInfoEndpoint(ui -> ui
                                .userService(gitHubEmailOAuth2UserService) // fetches real email for GitHub
                                                                           // private-email users
                        )
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler))
                // Filter order (outermost first):
                // 1. AuthRateLimitingFilter — blocks brute-force BEFORE any auth logic runs
                // 2. ApiKeyFilter — handles API key auth
                // 3. OAuthAccessTokenFilter — handles OAuth access token auth
                // 4. JWTFilter — handles JWT auth
                .addFilterBefore(authRateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(oauthAccessTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    /**
     * Bean for authentication manager.
     * This manager is used to handle authentication requests.
     * 
     * @param configuration the authentication configuration
     * @return an AuthenticationManager instance
     * @throws Exception if an error occurs during authentication manager creation
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager(); // expose for manual auth flows if needed
    }

    /**
     * Bean for DAO(Data Access Object) authentication provider.
     * This provider uses the user details service and password encoder for
     * authentication.
     * 
     * @return an AuthenticationProvider instance
     */
    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
            BCryptPasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * Defines CORS policy — which origins, methods, and headers are allowed for
     * cross-origin requests.
     * Allowed origins are read from application properties so they can differ per
     * environment.
     * AllowCredentials = true is required for cross-origin requests that send
     * cookies (refresh token cookie).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        /// Always use allowedOriginPatterns — setAllowedOrigins("*") is incompatible
        /// with allowCredentials=true and throws at runtime.
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            throw new IllegalStateException("CORS allowed origins must be configured");
        }
        cfg.setAllowedOriginPatterns(allowedOrigins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Guest-Session",
                "Idempotency-Key"));
        cfg.setExposedHeaders(List.of("Location"));
        cfg.setAllowCredentials(true); // required if refresh cookie used cross-origin (adjust in prod)
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
