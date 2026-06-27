package com.crescendo.apps.mailchimp;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Mailchimp Member handlers.
 * Operations (from n8n Mailchimp.node.ts, resource='member'):
 *   - createOrUpdate : PUT /3.0/lists/{listId}/members/{subscriberHash}
 *   - delete         : DELETE /3.0/lists/{listId}/members/{subscriberHash}
 *   - get            : GET /3.0/lists/{listId}/members/{subscriberHash}
 *   - getAll         : GET /3.0/lists/{listId}/members
 *   - update         : PATCH /3.0/lists/{listId}/members/{subscriberHash}
 *
 * Note: Mailchimp uses MD5 hash of lowercase email as subscriberHash.
 * Status values: subscribed, unsubscribed, cleaned, pending, transactional
 */
@Component
public class MailchimpMemberHandlers {

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

    /**
     * Mailchimp requires an MD5 hash of the subscriber's lowercase email address as the member ID.
     */
    private String getSubscriberHash(String email) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(email.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @ActionMapping(appKey = "mailchimp", actionKey = "mailchimp:member:createOrUpdate")
    public Object createOrUpdateMember(ActionContext context) throws Exception {
        String listId = context.getString("listId");
        String email = context.getString("email");
        String status = context.getString("status");
        Map<String, Object> mergeFields = context.getMap("mergeFields");
        Map<String, Object> additionalFields = context.getMap("additionalFields");

        String subscriberHash = getSubscriberHash(email);

        Map<String, Object> body = new HashMap<>();
        body.put("email_address", email);
        body.put("status_if_new", status);
        body.put("status", status);
        if (mergeFields != null && !mergeFields.isEmpty()) body.put("merge_fields", mergeFields);
        if (additionalFields != null) body.putAll(additionalFields);

        return RestClient.builder()
                .url(getBaseUrl(context) + "/lists/" + listId + "/members/" + subscriberHash)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .put(body)
                .execute();
    }

    @ActionMapping(appKey = "mailchimp", actionKey = "mailchimp:member:delete")
    public Object deleteMember(ActionContext context) throws Exception {
        String listId = context.getString("listId");
        String email = context.getString("email");
        String subscriberHash = getSubscriberHash(email);

        return RestClient.builder()
                .url(getBaseUrl(context) + "/lists/" + listId + "/members/" + subscriberHash + "/actions/delete-permanent")
                .header("Authorization", getAuth(context))
                .post(Map.of())
                .execute();
    }

    @ActionMapping(appKey = "mailchimp", actionKey = "mailchimp:member:get")
    public Object getMember(ActionContext context) throws Exception {
        String listId = context.getString("listId");
        String email = context.getString("email");
        String subscriberHash = getSubscriberHash(email);

        return RestClient.builder()
                .url(getBaseUrl(context) + "/lists/" + listId + "/members/" + subscriberHash)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "mailchimp", actionKey = "mailchimp:member:getAll")
    public Object getAllMembers(ActionContext context) throws Exception {
        String listId = context.getString("listId");
        int limit = context.getInt("limit", 100);
        String status = context.getString("status");

        StringBuilder url = new StringBuilder(getBaseUrl(context) + "/lists/" + listId + "/members?count=" + limit);
        if (status != null && !status.isBlank()) url.append("&status=").append(status);

        return RestClient.builder()
                .url(url.toString())
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "mailchimp", actionKey = "mailchimp:member:update")
    public Object updateMember(ActionContext context) throws Exception {
        String listId = context.getString("listId");
        String email = context.getString("email");
        String status = context.getString("status");
        Map<String, Object> mergeFields = context.getMap("mergeFields");
        Map<String, Object> additionalFields = context.getMap("additionalFields");

        String subscriberHash = getSubscriberHash(email);

        Map<String, Object> body = new HashMap<>();
        if (status != null) body.put("status", status);
        if (mergeFields != null && !mergeFields.isEmpty()) body.put("merge_fields", mergeFields);
        if (additionalFields != null) body.putAll(additionalFields);

        return RestClient.builder()
                .url(getBaseUrl(context) + "/lists/" + listId + "/members/" + subscriberHash)
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .patch(body)
                .execute();
    }
}
