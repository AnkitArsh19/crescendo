package com.crescendo.apps.github;

import com.crescendo.execution.action.ActionContext;

/**
 * Support class for GitHub authentication and API calls.
 */
public class GitHubSupport {

    public static String getBaseUrl() {
        return "https://api.github.com";
    }

    public static String getAuthHeader(ActionContext context) {
        String token = context.getCredential("accessToken");
        if (token == null) {
            token = context.getCredential("personalAccessToken");
        }
        return "Bearer " + token;
    }
}
