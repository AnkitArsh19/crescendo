package com.crescendo.apps.github;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Fetches GitHub resources (repos, branches, labels) using the connected OAuth token.
 * <p>
 * Cascade: repos → branches (dependent on selected repo).
 */
@Component
public class GitHubResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(GitHubResourceProvider.class);
    private static final String GITHUB_API = "https://api.github.com";

    private final RestClient restClient;

    public GitHubResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "github";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("repos", "branches", "labels", "issues");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String token = extractToken(credentials);

        return switch (resourceType) {
            case "repos" -> listRepos(token);
            case "branches" -> listBranches(token, requireParam(params, "repo"));
            case "labels" -> listLabels(token, requireParam(params, "repo"));
            case "issues" -> listIssues(token, requireParam(params, "repo"));
            default -> List.of();
        };
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listRepos(String token) {
        try {
            List<Map<String, Object>> repos = restClient.get()
                    .uri(GITHUB_API + "/user/repos?sort=updated&per_page=100&affiliation=owner,collaborator,organization_member")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .retrieve()
                    .body(List.class);

            if (repos == null) return List.of();

            return repos.stream()
                    .map(r -> {
                        String fullName = String.valueOf(r.get("full_name"));
                        String description = r.get("description") != null ? String.valueOf(r.get("description")) : "";
                        boolean isPrivate = Boolean.TRUE.equals(r.get("private"));
                        String label = (isPrivate ? "🔒 " : "") + fullName;
                        return new ResourceOption(fullName, label, description);
                    })
                    .toList();

        } catch (Exception e) {
            logger.error("[github] Failed to list repos: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listBranches(String token, String repo) {
        try {
            List<Map<String, Object>> branches = restClient.get()
                    .uri(GITHUB_API + "/repos/" + repo + "/branches?per_page=100")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .retrieve()
                    .body(List.class);

            if (branches == null) return List.of();

            return branches.stream()
                    .map(b -> {
                        String name = String.valueOf(b.get("name"));
                        boolean isProtected = Boolean.TRUE.equals(b.get("protected"));
                        return new ResourceOption(name, name, isProtected ? "🛡 Protected" : null);
                    })
                    .toList();

        } catch (Exception e) {
            logger.error("[github] Failed to list branches for {}: {}", repo, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listLabels(String token, String repo) {
        try {
            List<Map<String, Object>> labels = restClient.get()
                    .uri(GITHUB_API + "/repos/" + repo + "/labels?per_page=100")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .retrieve()
                    .body(List.class);

            if (labels == null) return List.of();

            return labels.stream()
                    .map(l -> new ResourceOption(
                            String.valueOf(l.get("name")),
                            String.valueOf(l.get("name")),
                            l.get("description") != null ? String.valueOf(l.get("description")) : null))
                    .toList();

        } catch (Exception e) {
            logger.error("[github] Failed to list labels for {}: {}", repo, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listIssues(String token, String repo) {
        try {
            List<Map<String, Object>> issues = restClient.get()
                    .uri(GITHUB_API + "/repos/" + repo + "/issues?state=open&per_page=100")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .retrieve()
                    .body(List.class);

            if (issues == null) return List.of();

            return issues.stream()
                    .filter(i -> !i.containsKey("pull_request")) // Exclude PRs
                    .map(i -> new ResourceOption(
                            String.valueOf(i.get("number")),
                            "#" + i.get("number") + " " + i.get("title"),
                            i.get("state") != null ? "State: " + i.get("state") : null))
                    .toList();

        } catch (Exception e) {
            logger.error("[github] Failed to list issues for {}: {}", repo, e.getMessage());
            return List.of();
        }
    }

    private String extractToken(Map<String, Object> credentials) {
        Object token = credentials.get("accessToken");
        if (token == null || token.toString().isBlank()) {
            throw new IllegalArgumentException("GitHub connection is missing 'accessToken'");
        }
        return token.toString();
    }

    private String requireParam(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required parameter '" + key + "' is missing");
        }
        return value;
    }
}
