package com.crescendo.apps.notion;

import com.crescendo.execution.action.ActionContext;

/**
 * Support class for Notion authentication and API calls.
 */
public class NotionSupport {

    public static String getBaseUrl() {
        return "https://api.notion.com/v1";
    }

    public static String getAuthHeader(ActionContext context) {
        String token = context.getCredential("apiToken");
        if (token == null) {
            token = context.getCredential("accessToken");
        }
        return "Bearer " + token;
    }

    public static String getVersionHeader() {
        return "2022-06-28";
    }
}
