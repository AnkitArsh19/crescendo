package com.crescendo.apps.jenkins;

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
 * Fetches Jenkins resources: jobs available on the server.
 * Authenticates via Username + API Token (HTTP Basic).
 */
@Component
@SuppressWarnings("unchecked")
public class JenkinsResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(JenkinsResourceProvider.class);

    private final RestClient restClient;

    public JenkinsResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "jenkins";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("jobs");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of(new ResourceContextDescriptor("jobs", 100, java.time.Duration.ofMinutes(3)));
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials, String resourceType,
            Map<String, String> params) {
        String baseUrl = str(credentials.get("baseUrl")).replaceAll("/+$", "");
        String username = str(credentials.get("username"));
        String apiToken = str(credentials.get("apiToken"));
        if (baseUrl.isBlank() || username.isBlank() || apiToken.isBlank()) {
            throw new IllegalArgumentException("Jenkins requires baseUrl, username, and apiToken credentials.");
        }
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((username + ":" + apiToken).getBytes());

        return switch (resourceType) {
            case "jobs" -> listJobs(baseUrl, basicAuth);
            default -> List.of();
        };
    }

    private List<ResourceOption> listJobs(String baseUrl, String auth) {
        try {
            Map<String, Object> resp = restClient.get()
                    .uri(baseUrl + "/api/json?tree=jobs[name,url,color,description]&depth=2")
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .retrieve().body(Map.class);
            if (resp == null) return List.of();
            List<Map<String, Object>> jobs = (List<Map<String, Object>>) resp.get("jobs");
            if (jobs == null) return List.of();
            List<ResourceOption> options = new ArrayList<>();
            collectJobs(jobs, "", options);
            return options;
        } catch (Exception e) {
            logger.error("[jenkins] Failed to list jobs: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private void collectJobs(List<Map<String, Object>> jobs, String prefix, List<ResourceOption> options) {
        for (Map<String, Object> job : jobs) {
            String name = str(job.get("name"));
            String fullName = prefix.isBlank() ? name : prefix + "/" + name;
            String color = str(job.get("color"));
            String statusLabel = switch (color) {
                case "blue" -> "✅ Stable";
                case "red" -> "❌ Failed";
                case "yellow" -> "⚠️ Unstable";
                case "grey", "disabled" -> "⏸ Disabled";
                default -> "⚪ Unknown";
            };
            options.add(new ResourceOption(fullName, fullName, statusLabel));
            // Recurse into folders
            List<Map<String, Object>> nested = (List<Map<String, Object>>) job.get("jobs");
            if (nested != null) {
                collectJobs(nested, fullName, options);
            }
        }
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
