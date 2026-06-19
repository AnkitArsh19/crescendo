package com.crescendo.execution.trigger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Polling triggers for GitHub personal/OAuth tokens.
 */
@Component
public class GitHubTriggerPoller implements TriggerPoller {
    private static final Logger logger = LoggerFactory.getLogger(GitHubTriggerPoller.class);
    private static final String API = "https://api.github.com";

    private final RestClient restClient = RestClient.create();

    @Override
    public boolean supports(String appKey, String triggerKey) {
        return "github".equals(appKey)
                && List.of("new-issue", "new-pr", "push", "new-release", "new-star").contains(triggerKey);
    }

    @Override
    public List<Map<String, Object>> poll(Map<String, Object> credentials,
                                          Map<String, Object> configuration,
                                          Instant lastPollTime) {
        String token = token(credentials);
        String repo = required(configuration, "repo");
        String triggerKey = String.valueOf(configuration.getOrDefault("triggerKey", ""));

        try {
            if ("new-issue".equals(triggerKey)) {
                return pollIssues(token, repo, lastPollTime);
            }
            if ("new-pr".equals(triggerKey)) {
                return pollPullRequests(token, repo, lastPollTime);
            }
            if ("push".equals(triggerKey)) {
                return pollCommits(token, repo, asString(configuration.get("branch")), lastPollTime);
            }
            if ("new-release".equals(triggerKey)) {
                return pollReleases(token, repo, lastPollTime);
            }
            if ("new-star".equals(triggerKey)) {
                return pollStars(token, repo, lastPollTime);
            }
        } catch (Exception e) {
            logger.error("[github-poller] Failed to poll {} for {}: {}", triggerKey, repo, e.getMessage());
        }

        return List.of();
    }

    private List<Map<String, Object>> pollIssues(String token, String repo, Instant since) {
        List<Map<String, Object>> issues = getList(token, API + "/repos/" + repo
                + "/issues?state=all&sort=created&direction=asc&per_page=50&since=" + encode(since.toString()),
                "application/vnd.github+json");
        return issues.stream()
                .filter(item -> !item.containsKey("pull_request"))
                .filter(item -> isAfter(item.get("created_at"), since))
                .map(item -> event(item, "created_at", "github", "new-issue"))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> pollPullRequests(String token, String repo, Instant since) {
        String query = encode("repo:" + repo + " type:pr created:>" + since);
        Map<String, Object> response = getMap(token, API + "/search/issues?q=" + query
                + "&sort=created&order=asc&per_page=50", "application/vnd.github+json");
        List<Map<String, Object>> items = response.get("items") instanceof List<?> list
                ? (List<Map<String, Object>>) list
                : List.of();
        return items.stream()
                .filter(item -> isAfter(item.get("created_at"), since))
                .map(item -> event(item, "created_at", "github", "new-pr"))
                .toList();
    }

    private List<Map<String, Object>> pollCommits(String token, String repo, String branch, Instant since) {
        StringBuilder url = new StringBuilder(API + "/repos/" + repo + "/commits?per_page=50&since=")
                .append(encode(since.toString()));
        if (branch != null && !branch.isBlank()) {
            url.append("&sha=").append(encode(branch));
        }
        List<Map<String, Object>> commits = getList(token, url.toString(), "application/vnd.github+json");
        return commits.stream()
                .filter(item -> isAfter(commitDate(item), since))
                .map(item -> event(item, "commit.author.date", "github", "push"))
                .toList();
    }

    private List<Map<String, Object>> pollReleases(String token, String repo, Instant since) {
        List<Map<String, Object>> releases = getList(token, API + "/repos/" + repo
                + "/releases?per_page=50", "application/vnd.github+json");
        return releases.stream()
                .filter(item -> isAfter(item.get("published_at"), since))
                .map(item -> event(item, "published_at", "github", "new-release"))
                .toList();
    }

    private List<Map<String, Object>> pollStars(String token, String repo, Instant since) {
        List<Map<String, Object>> stars = getList(token, API + "/repos/" + repo
                + "/stargazers?per_page=50", "application/vnd.github.star+json");
        return stars.stream()
                .filter(item -> isAfter(item.get("starred_at"), since))
                .map(item -> event(item, "starred_at", "github", "new-star"))
                .toList();
    }

    private Map<String, Object> event(Map<String, Object> item, String timestampField, String provider, String triggerKey) {
        String id = item.get("id") != null ? String.valueOf(item.get("id")) : String.valueOf(item.getOrDefault("sha", item.hashCode()));
        Object timestamp = switch (timestampField) {
            case "commit.author.date" -> commitDate(item);
            default -> item.get(timestampField);
        };
        return Map.of(
                "id", id,
                "provider", provider,
                "triggerKey", triggerKey,
                "createdAt", timestamp != null ? String.valueOf(timestamp) : Instant.now().toString(),
                "data", item
        );
    }

    private Object commitDate(Map<String, Object> item) {
        Object commit = item.get("commit");
        if (commit instanceof Map<?, ?> commitMap) {
            Object author = commitMap.get("author");
            if (author instanceof Map<?, ?> authorMap) {
                return authorMap.get("date");
            }
        }
        return item.get("created_at");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(String token, String url, String accept) {
        List<Map<String, Object>> response = restClient.get()
                .uri(URI.create(url))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(HttpHeaders.ACCEPT, accept)
                .retrieve()
                .body(List.class);
        return response != null ? response : List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(String token, String url, String accept) {
        Map<String, Object> response = restClient.get()
                .uri(URI.create(url))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(HttpHeaders.ACCEPT, accept)
                .retrieve()
                .body(Map.class);
        return response != null ? response : Map.of();
    }

    private boolean isAfter(Object timestamp, Instant since) {
        try {
            return timestamp != null && Instant.parse(String.valueOf(timestamp)).isAfter(since);
        } catch (Exception e) {
            return false;
        }
    }

    private String token(Map<String, Object> credentials) {
        Object token = credentials.get("accessToken");
        if (token == null || String.valueOf(token).isBlank()) {
            token = credentials.get("apiKey");
        }
        if (token == null || String.valueOf(token).isBlank()) {
            throw new IllegalArgumentException("GitHub connection is missing accessToken or apiKey");
        }
        return String.valueOf(token);
    }

    private String required(Map<String, Object> configuration, String key) {
        String value = asString(configuration.get(key));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("GitHub trigger requires '" + key + "'");
        }
        return value;
    }

    private String asString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
