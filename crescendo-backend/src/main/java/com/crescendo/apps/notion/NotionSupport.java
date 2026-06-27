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
        return "Bearer " + context.getCredential("apiToken");
    }

    public static String getVersionHeader() {
        return "2022-06-28";
    }
}
