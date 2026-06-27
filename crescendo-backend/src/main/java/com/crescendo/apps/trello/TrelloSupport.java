package com.crescendo.apps.trello;

import com.crescendo.execution.action.ActionContext;

/**
 * Support class for Trello authentication.
 */
public class TrelloSupport {

    public static String getBaseUrl() {
        return "https://api.trello.com/1";
    }

    public static String getAuthQuery(ActionContext context) {
        String apiKey = context.getCredential("apiKey");
        String apiToken = context.getCredential("apiToken");
        return "?key=" + apiKey + "&token=" + apiToken;
    }
    
    public static String getAuthQuery(ActionContext context, boolean firstParam) {
        String apiKey = context.getCredential("apiKey");
        String apiToken = context.getCredential("apiToken");
        String prefix = firstParam ? "?" : "&";
        return prefix + "key=" + apiKey + "&token=" + apiToken;
    }
}
