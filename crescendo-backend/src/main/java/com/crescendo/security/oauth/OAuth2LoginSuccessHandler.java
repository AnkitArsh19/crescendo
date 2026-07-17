package com.crescendo.security.oauth;

import com.crescendo.auth.dto.AuthDto;
import com.crescendo.auth.service.AuthenticationService;
import com.crescendo.enums.AuthProvider;
import com.crescendo.security.RefreshTokenCookieService;
import com.crescendo.security.mfa.MFAService;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import com.crescendo.user.user_command.user_identity.UserIdentity;
import com.crescendo.user.user_command.user_identity.UserIdentityRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles the callback after a successful OAuth2 login (Google / GitHub).
 *
 * Flow:
 *   1. Spring Security completes the OAuth2 Authorization Code exchange.
 *   2. This handler fires with the authenticated OAuth2User principal.
 *   3. We extract the provider user ID + email, call AuthenticationService.oauthLogin()
 *      which creates or links the account and issues JWT tokens.
 *   4. We redirect to the frontend with the access token in a URL fragment,
 *      and set the refresh token as an HttpOnly cookie.
 *
 * MFA gating:
 *   If the linked account has MFA enabled, we redirect to the frontend MFA challenge page
 *   instead of issuing tokens immediately. The frontend then calls POST /mfa/challenge.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthenticationService authService;
    private final MFAService mfaService;
    private final RefreshTokenCookieService cookieService;
    private final User_commandRepository userRepo;
    private final UserIdentityRepository identityRepo;

    // Frontend URL to redirect to after OAuth success. Configured in application.properties.
    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.cookie.secure:false}")
    private boolean secureCookie;

    @Value("${jwt.refresh.expiration}")
    private long refreshExpirationMs;

    public OAuth2LoginSuccessHandler(AuthenticationService authService,
                                     MFAService mfaService,
                                     RefreshTokenCookieService cookieService,
                                     User_commandRepository userRepo,
                                     UserIdentityRepository identityRepo) {
        this.authService = authService;
        this.mfaService = mfaService;
        this.cookieService = cookieService;
        this.userRepo = userRepo;
        this.identityRepo = identityRepo;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = oauthToken.getPrincipal();
        String registrationId = oauthToken.getAuthorizedClientRegistrationId(); // "google" or "github"

        // Map the Spring registration ID to our AuthProvider enum.
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        // Extract provider-specific fields (user ID, email, display name).
        String providerUserId = extractProviderUserId(oauthUser, registrationId);
        String email = extractEmail(oauthUser, registrationId);
        String displayName = extractDisplayName(oauthUser, registrationId);

        // Check if this is a provider-linking flow (initiated from Connected Accounts settings).
        // The link_user_id cookie is set by POST /users/me/link-provider/init before the OAuth redirect.
        String linkUserId = extractLinkCookie(request);
        if (linkUserId != null) {
            handleLinkProvider(response, linkUserId, provider, providerUserId, email);
            return;
        }

        // Standard login flow below

        // Check if this OAuth identity is already linked and if MFA is active for that user.
        // We do this BEFORE issuing tokens so MFA-enabled accounts can't be bypassed via OAuth.
        Optional<User_command> existingUser = findExistingUser(provider, providerUserId, email);
        if (existingUser.isPresent() && mfaService.hasEnabledMfa(existingUser.get().getId())) {
            // MFA is required, redirect to the frontend MFA challenge page with the email.
            // No tokens issued yet. The frontend will call POST /mfa/challenge.
            String encoded = URLEncoder.encode(existingUser.get().getEmailId(), StandardCharsets.UTF_8);
            response.sendRedirect(frontendUrl + "/mfa/challenge?email=" + encoded + "&oauth=true");
            return;
        }

        // Read device fingerprint from a short-lived transfer cookie set by the frontend
        // immediately before initiating the OAuth redirect (SameSite=Lax allows it to
        // survive the cross-origin redirect chain).
        String deviceId = extractCookieValue(request, "crescendo_device_id_transfer");
        String deviceLabel = extractCookieValue(request, "crescendo_device_label_transfer");
        String clientIp = extractClientIp(request);

        // No MFA — proceed to issue tokens.
        String userAgent = request.getHeader("User-Agent");
        AuthDto.LoginResponse loginResp = authService.oauthLogin(
                provider, providerUserId, email, displayName, userAgent,
                clientIp, deviceId, deviceLabel
        );

        // Set refresh token as HttpOnly cookie.
        long ttlMs = Duration.between(Instant.now(), loginResp.refreshExpiresAt()).toMillis();
        cookieService.setRefreshToken(response, loginResp.refreshToken(), ttlMs, secureCookie);

        // Redirect to the frontend callback page with the access token in the URL fragment.
        // Fragment (#) is never sent to servers in HTTP requests, providing an extra layer of safety.
        // The frontend reads it from window.location.hash and stores it in memory.
        String redirectUrl = frontendUrl + "/oauth/callback"
                + "#access_token=" + loginResp.accessToken()
                + "&expires_at=" + URLEncoder.encode(loginResp.accessExpiresAt().toString(), StandardCharsets.UTF_8);
        response.sendRedirect(redirectUrl);
    }

    /// Handles the provider-linking flow: adds the OAuth identity to an existing user account
    /// without issuing new tokens. The user's existing session remains valid.
    /// NOTE: No @Transactional here — private methods bypass Spring AOP proxies.
    /// This method runs inside the outer transaction from onAuthenticationSuccess.
    private void handleLinkProvider(HttpServletResponse response, String userIdStr,
                                    AuthProvider provider, String providerUserId, String email)
            throws IOException {

        // Clear the link cookie immediately — single-use.
        ResponseCookie clearCookie = ResponseCookie.from("link_user_id", "")
                .httpOnly(true).secure(false).path("/").maxAge(Duration.ZERO).build();
        response.addHeader("Set-Cookie", clearCookie.toString());

        UUID userId = UUID.fromString(userIdStr);
        User_command user = userRepo.findById(userId).orElse(null);
        if (user == null) {
            response.sendRedirect(frontendUrl + "/dashboard/settings/accounts?error=user_not_found");
            return;
        }

        // Check if this exact provider identity is already linked to ANY account.
        Optional<UserIdentity> existingIdentity = identityRepo.findByProviderAndProviderUserId(provider, providerUserId);
        if (existingIdentity.isPresent()) {
            if (existingIdentity.get().getUser().getId().equals(userId)) {
                // Already linked to this user — just redirect back.
                response.sendRedirect(frontendUrl + "/dashboard/settings/accounts?linked=" + provider.name().toLowerCase());
            } else {
                // This OAuth identity belongs to a different account — can't link.
                response.sendRedirect(frontendUrl + "/dashboard/settings/accounts?error=provider_linked_to_other_account");
            }
            return;
        }

        // Link the new provider to the current user's account.
        identityRepo.save(new UserIdentity(UUID.randomUUID(), user, provider, providerUserId, email));

        // Redirect back to settings — the frontend will call checkAuth() to refresh data.
        response.sendRedirect(frontendUrl + "/dashboard/settings/accounts?linked=" + provider.name().toLowerCase());
    }

    /// Reads the link_user_id cookie set by POST /users/me/link-provider/init.
    /// Returns the user ID string if the cookie exists, or null for a normal login flow.
    private String extractLinkCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if ("link_user_id".equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                return c.getValue();
            }
        }
        return null;
    }

    // PROVIDER-SPECIFIC FIELD EXTRACTION

    /// Extracts the unique user identifier from the OAuth provider's response.
    /// Google uses "sub" (OpenID Connect subject), GitHub uses "id" (integer).
    private String extractProviderUserId(OAuth2User user, String registrationId) {
        return switch (registrationId) {
            case "google" -> user.getAttribute("sub");
            case "github" -> {
                Object id = user.getAttribute("id");
                yield id != null ? id.toString() : null;
            }
            default -> throw new IllegalArgumentException("Unsupported OAuth provider: " + registrationId);
        };
    }

    /// Extracts the email address from the OAuth provider's response.
    /// Google always returns "email" in the standard OIDC claims.
    /// GitHub may have a null/private email — we fall back to "<username>@users.noreply.github.com".
    private String extractEmail(OAuth2User user, String registrationId) {
        String email = user.getAttribute("email");
        if (email != null) return email;

        if ("github".equals(registrationId)) {
            // GitHub users with private emails: construct a no-reply address.
            // This is the same pattern GitHub uses for commits from private-email accounts.
            String login = user.getAttribute("login");
            return login + "@users.noreply.github.com";
        }
        return null;
    }

    /// Extracts a display name suitable as a default username.
    /// Preserves the provider's display name as-is (spaces, casing, etc.).
    /// Falls back through "name" → "login" (GitHub) → email prefix → "user".
    private String extractDisplayName(OAuth2User user, String registrationId) {
        String name = user.getAttribute("name");
        if (name != null && !name.isBlank()) return truncateUsername(name.strip());

        if ("github".equals(registrationId)) {
            String login = user.getAttribute("login");
            if (login != null && !login.isBlank()) return login.strip();
        }

        String email = user.getAttribute("email");
        if (email != null) return email.split("@")[0];
        return "user";
    }

    /// Truncates a display name to 100 characters max (matching Username VO limit).
    private String truncateUsername(String name) {
        if (name.isEmpty()) return "user";
        return name.length() > 100 ? name.substring(0, 100) : name;
    }

    /// Looks up the user by OAuth identity or by email (for account linking).
    /// Used to check MFA status before issuing tokens.
    /// Eagerly loads the User_command entity via userRepo.findById() to avoid
    /// LazyInitializationException when accessing user fields outside a session.
    private Optional<User_command> findExistingUser(AuthProvider provider, String providerUserId, String email) {
        // First try exact OAuth identity match.
        Optional<UserIdentity> identity = identityRepo.findByProviderAndProviderUserId(provider, providerUserId);
        // Load the full entity — identity.getUser() returns a lazy proxy whose
        // getId() is safe (Hibernate knows the FK without loading), but other
        // fields like emailId would fail outside a session.
        return identity.map(userIdentity -> userRepo.findById(userIdentity.getUser().getId())).orElseGet(() -> email != null ? userRepo.findByEmailIgnoreCase(email) : Optional.empty());
        // Fall back to email match (account linking scenario).
    }

    /// Reads a named cookie from the request. Returns null if absent.
    private String extractCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    /// Extracts the real client IP, honouring X-Forwarded-For set by a reverse proxy.
    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}

