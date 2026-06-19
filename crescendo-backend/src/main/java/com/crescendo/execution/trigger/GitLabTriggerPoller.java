package com.crescendo.execution.trigger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * Polling triggers for GitLab personal/OAuth tokens using GitLab REST API v4.
 */
@Component
public class GitLabTriggerPoller implements TriggerPoller {
    private static final Logger logger = LoggerFactory.getLogger(GitLabTriggerPoller.class);
    private static final String API = "https://gitlab.com/api/v4";

    private final RestClient restClient = RestClient.create();

    @Override
    public boolean supports(String appKey, String triggerKey) {
        return "gitlab".equals(appKey)
                && List.of("new-issue", "new-mr", "push", "new-comment", "pipeline-status").contains(triggerKey);
    }

    @Override
    public List<Map<String, Object>> poll(Map<String, Object> credentials,
                                          Map<String, Object> configuration,
                                          Instant lastPollTime) {
        String token = token(credentials);
        String projectId = required(configuration, "projectId");
        String triggerKey = String.valueOf(configuration.getOrDefault("triggerKey", ""));

        try {
            if ("new-issue".equals(triggerKey)) {
                return pollIssues(token, projectId, lastPollTime);
            }
            if ("new-mr".equals(triggerKey)) {
                return pollMergeRequests(token, projectId, lastPollTime);
            }
            if ("push".equals(triggerKey)) {
                return pollCommits(token, projectId, asString(configuration.get("branch")), lastPollTime);
            }
            if ("new-comment".equals(triggerKey)) {
                return pollCommentEvents(token, projectId, lastPollTime);
            }
            if ("pipeline-status".equals(triggerKey)) {
                return pollPipelines(token, projectId, lastPollTime);
            }
        } catch (Exception e) {
            logger.error("[gitlab-poller] Failed to poll {} for project {}: {}", triggerKey, projectId, e.getMessage());
        }

        return List.of();
    }

    private List<Map<String, Object>> pollIssues(String token, String projectId, Instant since) {
        List<Map<String, Object>> issues = getList(token, API + "/projects/" + encodePath(projectId)
                + "/issues?scope=all&order_by=created_at&sort=asc&per_page=50&created_after=" + encode(since.toString()));
        return issues.stream()
                .filter(item -> isAfter(item.get("created_at"), since))
                .map(item -> event(item, "created_at", "new-issue"))
                .toList();
    }

    private List<Map<String, Object>> pollMergeRequests(String token, String projectId, Instant since) {
        List<Map<String, Object>> mrs = getList(token, API + "/projects/" + encodePath(projectId)
                + "/merge_requests?scope=all&order_by=created_at&sort=asc&per_page=50&created_after=" + encode(since.toString()));
        return mrs.stream()
                .filter(item -> isAfter(item.get("created_at"), since))
                .map(item -> event(item, "created_at", "new-mr"))
                .toList();
    }

    private List<Map<String, Object>> pollCommits(String token, String projectId, String branch, Instant since) {
        StringBuilder url = new StringBuilder(API + "/projects/" + encodePath(projectId)
                + "/repository/commits?per_page=50&since=" + encode(since.toString()));
        if (branch != null && !branch.isBlank()) {
            url.append("&ref_name=").append(encode(branch));
        }
        List<Map<String, Object>> commits = getList(token, url.toString());
        return commits.stream()
                .filter(item -> isAfter(item.get("created_at"), since))
                .map(item -> event(item, "created_at", "push"))
                .toList();
    }

    private List<Map<String, Object>> pollCommentEvents(String token, String projectId, Instant since) {
        LocalDate afterDate = LocalDate.ofInstant(since.minusSeconds(86_400), ZoneOffset.UTC);
        List<Map<String, Object>> events = getList(token, API + "/projects/" + encodePath(projectId)
                + "/events?action=commented&sort=asc&per_page=50&after=" + afterDate);
        return events.stream()
                .filter(item -> isAfter(item.get("created_at"), since))
                .map(item -> event(item, "created_at", "new-comment"))
                .toList();
    }

    private List<Map<String, Object>> pollPipelines(String token, String projectId, Instant since) {
        List<Map<String, Object>> pipelines = getList(token, API + "/projects/" + encodePath(projectId)
                + "/pipelines?order_by=updated_at&sort=asc&per_page=50&updated_after=" + encode(since.toString()));
        return pipelines.stream()
                .filter(item -> isAfter(item.get("updated_at"), since) || isAfter(item.get("created_at"), since))
                .map(item -> event(item, item.get("updated_at") != null ? "updated_at" : "created_at", "pipeline-status"))
                .toList();
    }

    private Map<String, Object> event(Map<String, Object> item, String timestampField, String triggerKey) {
        Object timestamp = item.get(timestampField);
        Object id = item.getOrDefault("id", item.getOrDefault("sha", item.hashCode()));
        return Map.of(
                "id", String.valueOf(id),
                "provider", "gitlab",
                "triggerKey", triggerKey,
                "createdAt", timestamp != null ? String.valueOf(timestamp) : Instant.now().toString(),
                "data", item
        );
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(String token, String url) {
        List<Map<String, Object>> response = restClient.get()
                .uri(URI.create(url))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .body(List.class);
        return response != null ? response : List.of();
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
            throw new IllegalArgumentException("GitLab connection is missing accessToken or apiKey");
        }
        return String.valueOf(token);
    }

    private String required(Map<String, Object> configuration, String key) {
        String value = asString(configuration.get(key));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("GitLab trigger requires '" + key + "'");
        }
        return value;
    }

    private String asString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String encodePath(String value) {
        return encode(value).replace("%2F", "%2F");
    }
}
