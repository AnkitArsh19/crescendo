package com.crescendo.apps.github;

import com.crescendo.execution.action.ActionContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GitHubIssueHandlersTest {

    private final GitHubIssueHandlers handlers = new GitHubIssueHandlers();

    private ActionContext createContext(Map<String, Object> config, Map<String, Object> credentials) {
        return new ActionContext("github", "github:issue:create", config, credentials, Map.of(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1);
    }

    @Test
    @DisplayName("GitHubSupport creates Bearer auth header from accessToken")
    void getAuthHeader_fromAccessToken() {
        ActionContext context = createContext(Map.of(), Map.of("accessToken", "ghp_123456789"));
        String header = GitHubSupport.getAuthHeader(context);
        assertEquals("Bearer ghp_123456789", header);
    }

    @Test
    @DisplayName("GitHubSupport creates Bearer auth header from personalAccessToken")
    void getAuthHeader_fromPersonalAccessToken() {
        ActionContext context = createContext(Map.of(), Map.of("personalAccessToken", "ghp_pat_987654"));
        String header = GitHubSupport.getAuthHeader(context);
        assertEquals("Bearer ghp_pat_987654", header);
    }
}
