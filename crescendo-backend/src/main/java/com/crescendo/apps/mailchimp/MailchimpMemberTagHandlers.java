package com.crescendo.apps.mailchimp;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Mailchimp Member Tag handlers.
 * Operations (from n8n Mailchimp.node.ts, resource='memberTag'):
 *   - add    : POST /3.0/lists/{listId}/members/{subscriberHash}/tags  (status: active)
 *   - remove : POST /3.0/lists/{listId}/members/{subscriberHash}/tags  (status: inactive)
 *
 * n8n sends both add and remove via the same endpoint with tag status toggling.
 */
@Component
public class MailchimpMemberTagHandlers {

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

    private String getSubscriberHash(String email) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(email.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    @ActionMapping(appKey = "mailchimp", actionKey = "mailchimp:memberTag:add")
    public Object addTags(ActionContext context) throws Exception {
        String listId = context.getString("listId");
        String email = context.getString("email");
        String tagsRaw = context.getString("tags");

        String subscriberHash = getSubscriberHash(email);
        List<Map<String, Object>> tagList = new ArrayList<>();
        if (tagsRaw != null) {
            for (String tag : tagsRaw.split(",")) {
                String trimmed = tag.trim();
                if (!trimmed.isEmpty()) {
                    tagList.add(Map.of("name", trimmed, "status", "active"));
                }
            }
        }

        Map<String, Object> body = Map.of("tags", tagList);

        return RestClient.builder()
                .url(getBaseUrl(context) + "/lists/" + listId + "/members/" + subscriberHash + "/tags")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "mailchimp", actionKey = "mailchimp:memberTag:remove")
    public Object removeTags(ActionContext context) throws Exception {
        String listId = context.getString("listId");
        String email = context.getString("email");
        String tagsRaw = context.getString("tags");

        String subscriberHash = getSubscriberHash(email);
        List<Map<String, Object>> tagList = new ArrayList<>();
        if (tagsRaw != null) {
            for (String tag : tagsRaw.split(",")) {
                String trimmed = tag.trim();
                if (!trimmed.isEmpty()) {
                    tagList.add(Map.of("name", trimmed, "status", "inactive"));
                }
            }
        }

        Map<String, Object> body = Map.of("tags", tagList);

        return RestClient.builder()
                .url(getBaseUrl(context) + "/lists/" + listId + "/members/" + subscriberHash + "/tags")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }
}
