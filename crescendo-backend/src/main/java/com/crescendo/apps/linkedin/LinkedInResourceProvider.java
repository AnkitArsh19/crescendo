package com.crescendo.apps.linkedin;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceContextDescriptor;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Fetches LinkedIn resources: current user profile URN and administered organization pages.
 * Authenticates via OAuth2 access token.
 */
@Component
@SuppressWarnings("unchecked")
public class LinkedInResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(LinkedInResourceProvider.class);
    private static final String LINKEDIN_API = "https://api.linkedin.com/v2";

    private final RestClient restClient;

    public LinkedInResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "linkedin";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("authors", "organizations");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of(new ResourceContextDescriptor("authors", 50, java.time.Duration.ofMinutes(10)));
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials, String resourceType,
            Map<String, String> params) {
        String token = getToken(credentials);
        return switch (resourceType) {
            case "authors"       -> listAuthors(token);
            case "organizations" -> listOrganizations(token);
            default -> List.of();
        };
    }

    private List<ResourceOption> listAuthors(String token) {
        List<ResourceOption> options = new ArrayList<>();
        // 1. Personal profile
        try {
            Map<String, Object> userInfo = restClient.get()
                    .uri(LINKEDIN_API + "/userinfo")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (userInfo != null) {
                String sub = str(userInfo.get("sub"));
                String name = str(userInfo.get("name"));
                String email = str(userInfo.get("email"));
                String label = name.isBlank() ? "Personal Profile" : name + " (Personal)";
                options.add(new ResourceOption("urn:li:person:" + sub, label, email));
            }
        } catch (Exception e) {
            // Try legacy /v2/me
            try {
                Map<String, Object> me = restClient.get()
                        .uri(LINKEDIN_API + "/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve().body(Map.class);
                if (me != null) {
                    String id = str(me.get("id"));
                    String fn = str(me.get("localizedFirstName"));
                    String ln = str(me.get("localizedLastName"));
                    options.add(new ResourceOption("urn:li:person:" + id, (fn + " " + ln).trim() + " (Personal)", id));
                }
            } catch (Exception e2) {
                logger.error("[linkedin] Failed to get user profile: {}", e2.getMessage());
            }
        }

        // 2. Add any company pages
        options.addAll(listOrganizations(token));
        return options;
    }

    private List<ResourceOption> listOrganizations(String token) {
        try {
            Map<String, Object> acls = restClient.get()
                    .uri(LINKEDIN_API + "/organizationAcls?q=roleAssignee")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (acls == null || !acls.containsKey("elements")) return List.of();
            List<Map<String, Object>> elements = (List<Map<String, Object>>) acls.get("elements");
            if (elements == null) return List.of();

            return elements.stream().map(elem -> {
                String orgUrn = str(elem.get("organization"));
                String role = str(elem.get("role"));
                return new ResourceOption(orgUrn, "Organization " + orgUrn, "Role: " + role);
            }).toList();
        } catch (Exception e) {
            logger.debug("[linkedin] Note: Organization ACLs check returned: {}", e.getMessage());
            return List.of();
        }
    }

    private String getToken(Map<String, Object> credentials) {
        if (credentials != null) {
            for (String key : new String[]{"accessToken", "token", "apiKey"}) {
                Object val = credentials.get(key);
                if (val != null && !val.toString().isBlank()) return val.toString();
            }
        }
        throw new IllegalArgumentException("LinkedIn connection requires an OAuth2 access token.");
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
