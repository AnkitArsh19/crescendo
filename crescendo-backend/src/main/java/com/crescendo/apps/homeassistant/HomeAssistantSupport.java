package com.crescendo.apps.homeassistant;

import com.crescendo.execution.action.ActionContext;

public class HomeAssistantSupport {

    public static String getBaseUrl(ActionContext context) {
        String baseUrl = context.getCredential("baseUrl");
        if (baseUrl != null && baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    public static String getAuth(ActionContext context) {
        return "Bearer " + context.getCredential("accessToken");
    }
}
