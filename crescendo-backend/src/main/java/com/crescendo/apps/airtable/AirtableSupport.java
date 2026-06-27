package com.crescendo.apps.airtable;

import com.crescendo.execution.action.ActionContext;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Support class for Airtable authentication and API calls.
 */
public class AirtableSupport {

    public static String getBaseUrl() {
        return "https://api.airtable.com/v0";
    }

    public static String getAuth(ActionContext context) {
        return "Bearer " + context.getCredential("apiToken");
    }

    public static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
