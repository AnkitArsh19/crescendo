package com.crescendo.apps.dropbox;

import com.crescendo.execution.action.ActionContext;

public class DropboxSupport {

    public static String getApiBaseUrl() {
        return "https://api.dropboxapi.com/2";
    }

    public static String getContentBaseUrl() {
        return "https://content.dropboxapi.com/2";
    }

    public static String getAuth(ActionContext context) {
        return "Bearer " + context.getCredential("accessToken");
    }
}
