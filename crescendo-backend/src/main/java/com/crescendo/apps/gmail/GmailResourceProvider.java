package com.crescendo.apps.gmail;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Fetches Gmail labels for dynamic dropdowns.
 * Supports: labels
 */
@Component
public class GmailResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(GmailResourceProvider.class);
    private static final String DEFAULT_GMAIL_API = "https://gmail.googleapis.com/gmail/v1";
    private final String gmailApi;

    public GmailResourceProvider() {
        this(DEFAULT_GMAIL_API);
    }

    /**
     * Explicit endpoint seam for deterministic provider-contract tests. Production
     * code uses the default Google endpoint; tests can point at a local fixture.
     */
    public GmailResourceProvider(String gmailApi) {
        this.gmailApi = gmailApi;
    }

    @Override
    public String appKey() {
        return "gmail";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("labels");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String accessToken = credentials.get("accessToken").toString();
        return listLabels(accessToken);
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listLabels(String accessToken) {
        try {
            Map<String, Object> response = restClient(accessToken)
                    .get()
                    .uri(gmailApi + "/users/me/labels")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> labels = (List<Map<String, Object>>) response.get("labels");
            if (labels == null) return List.of();

            return labels.stream()
                    .map(label -> new ResourceOption(
                            label.get("id").toString(),
                            label.get("name").toString(),
                            label.get("type") != null ? label.get("type").toString() : null
                    ))
                    .sorted(Comparator.comparing(ResourceOption::label))
                    .toList();
        } catch (Exception e) {
            logger.error("[gmail] Failed to list labels: {}", e.getMessage());
            return List.of();
        }
    }

    private RestClient restClient(String accessToken) {
        return RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
