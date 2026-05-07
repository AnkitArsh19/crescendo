package com.crescendo.security.oauth;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom OAuth2 user service that enriches GitHub users with their primary
 * verified email address.
 *
 * Problem: GitHub's user-info endpoint ({@code GET /user}) returns
 * {@code email: null} when the user has "Keep my email addresses private"
 * enabled. This causes downstream code to fall back to a {@code noreply}
 * address, which won't match the user's Google/local account email and
 * results in a duplicate account instead of linking providers together.
 *
 * Fix: With the {@code user:email} scope (already configured in
 * application.properties), we call {@code GET /user/emails} — GitHub's
 * dedicated endpoint that returns ALL of the user's email addresses —
 * and pick the primary + verified one.
 *
 * For non-GitHub providers (Google, etc.) we delegate to the default
 * service unchanged.
 */
@Component
public class GitHubEmailOAuth2UserService extends DefaultOAuth2UserService {

    private static final String GITHUB_EMAILS_URL = "https://api.github.com/user/emails";

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Delegate to Spring's default user-info loading first.
        OAuth2User user = super.loadUser(userRequest);

        // Only enrich for GitHub — Google already returns email via standard OIDC.
        if (!"github".equals(userRequest.getClientRegistration().getRegistrationId())) {
            return user;
        }

        // If GitHub already provided an email (user has it public), no extra call needed.
        String email = user.getAttribute("email");
        if (email != null && !email.isBlank()) {
            return user;
        }

        // Fetch the primary verified email from GitHub's /user/emails endpoint.
        String primaryEmail = fetchPrimaryEmail(userRequest.getAccessToken().getTokenValue());
        if (primaryEmail == null) {
            return user; // nothing we can do — fall back to noreply downstream
        }

        // Build a new OAuth2User with the email injected into the attributes map.
        Map<String, Object> enriched = new HashMap<>(user.getAttributes());
        enriched.put("email", primaryEmail);

        String nameAttributeKey = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName(); // "id" for GitHub

        return new DefaultOAuth2User(user.getAuthorities(), enriched, nameAttributeKey);
    }

    /// Calls GET https://api.github.com/user/emails and returns the primary + verified email,
    /// or null if the API call fails or no primary email is found.
    private String fetchPrimaryEmail(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    GITHUB_EMAILS_URL,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {}
            );

            if (response.getBody() == null) return null;

            // GitHub returns a list of email objects:
            //   { "email": "...", "primary": true/false, "verified": true/false, "visibility": ... }
            // We want the one that's both primary AND verified.
            for (Map<String, Object> emailObj : response.getBody()) {
                if (Boolean.TRUE.equals(emailObj.get("primary"))
                        && Boolean.TRUE.equals(emailObj.get("verified"))) {
                    return (String) emailObj.get("email");
                }
            }
        } catch (Exception e) {
            // Log but don't fail — noreply fallback in the success handler is still functional.
        }

        return null;
    }
}
