package com.crescendo.apps.mailchimp;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Mailchimp List Group (Interest Category) handlers.
 * Operations (from n8n Mailchimp.node.ts, resource='listGroup'):
 *   - create  : POST /3.0/lists/{listId}/interest-categories
 *   - delete  : DELETE /3.0/lists/{listId}/interest-categories/{interestCategoryId}
 *   - getAll  : GET /3.0/lists/{listId}/interest-categories
 */
@Component
public class MailchimpListGroupHandlers {

    private String getDataCenter(String apiKey) {
        if (apiKey != null && apiKey.contains("-")) {
            return apiKey.substring(apiKey.lastIndexOf('-') + 1);
        }
        return "us1";
    }

    private String getBaseUrl(ActionContext context) {
        String dc = getDataCenter(context.getCredential("apiKey"));
        return "https://" + dc + ".api.mailchimp.com/3.0";
    }

    private String getAuth(ActionContext context) {
        String encoded = Base64.getEncoder().encodeToString(("anystring:" + context.getCredential("apiKey")).getBytes());
        return "Basic " + encoded;
    }

    @ActionMapping(appKey = "mailchimp", actionKey = "mailchimp:listGroup:create")
    public Object createListGroup(ActionContext context) throws Exception {
        String listId = context.getString("listId");
        String name = context.getString("name");
        String type = context.getString("type"); // checkboxes, dropdown, radio, hidden
        String title = context.getString("title");

        Map<String, Object> body = new HashMap<>();
        body.put("title", title != null ? title : name);
        body.put("type", type);

        return RestClient.builder()
                .url(getBaseUrl(context) + "/lists/" + listId + "/interest-categories")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "mailchimp", actionKey = "mailchimp:listGroup:delete")
    public Object deleteListGroup(ActionContext context) throws Exception {
        String listId = context.getString("listId");
        String groupId = context.getString("groupId");

        return RestClient.builder()
                .url(getBaseUrl(context) + "/lists/" + listId + "/interest-categories/" + groupId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "mailchimp", actionKey = "mailchimp:listGroup:getAll")
    public Object getAllListGroups(ActionContext context) throws Exception {
        String listId = context.getString("listId");

        return RestClient.builder()
                .url(getBaseUrl(context) + "/lists/" + listId + "/interest-categories")
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }
}
