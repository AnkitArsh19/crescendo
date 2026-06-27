package com.crescendo.apps.wordpress;

import com.crescendo.execution.action.ActionContext;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class WordPressSupport {

    public static String getBaseUrl(ActionContext context) {
        String siteUrl = context.getCredential("siteUrl");
        if (siteUrl != null && siteUrl.endsWith("/")) {
            return siteUrl.substring(0, siteUrl.length() - 1);
        }
        return siteUrl;
    }

    public static String getAuth(ActionContext context) {
        String basic = context.getCredential("username") + ":" + context.getCredential("applicationPassword");
        return "Basic " + Base64.getEncoder().encodeToString(basic.getBytes(StandardCharsets.UTF_8));
    }
}
