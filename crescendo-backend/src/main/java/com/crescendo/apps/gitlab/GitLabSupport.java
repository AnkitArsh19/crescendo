package com.crescendo.apps.gitlab;

import com.crescendo.execution.action.ActionContext;

/**
 * Support class for GitLab authentication and API calls.
 */
public class GitLabSupport {

    public static String getBaseUrl(ActionContext context) {
        String server = context.getCredential("server");
        if (server == null || server.isBlank()) {
            server = "https://gitlab.com";
        }
        if (server.endsWith("/")) {
            server = server.substring(0, server.length() - 1);
        }
        return server + "/api/v4";
    }

    public static String getAuthHeader(ActionContext context) {
        String token = context.getCredential("accessToken");
        if (token == null) {
            token = context.getCredential("personalAccessToken");
        }
        return "Bearer " + token;
    }
}
