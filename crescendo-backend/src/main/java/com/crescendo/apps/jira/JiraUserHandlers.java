package com.crescendo.apps.jira;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Map;

/**
 * Jira User handlers.
 */
@Component
public class JiraUserHandlers {

    private String getBaseUrl(ActionContext context) {
        String domain = context.getCredential("domain");
        if (domain != null && domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        return domain + "/rest/api/3";
    }

    private String getAuth(ActionContext context) {
        String email = context.getCredential("email");
        String token = context.getCredential("apiToken");
        return "Basic " + Base64.getEncoder().encodeToString((email + ":" + token).getBytes());
    }

    @ActionMapping(appKey = "jira", actionKey = "jira:user:create")
    public Object createUser(ActionContext context) throws Exception {
        String email = context.getString("email");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/user")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(Map.of("emailAddress", email))
                .execute();
    }

    @ActionMapping(appKey = "jira", actionKey = "jira:user:get")
    public Object getUser(ActionContext context) throws Exception {
        String accountId = context.getString("accountId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/user?accountId=" + accountId)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "jira", actionKey = "jira:user:delete")
    public Object deleteUser(ActionContext context) throws Exception {
        String accountId = context.getString("accountId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/user?accountId=" + accountId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }
}
