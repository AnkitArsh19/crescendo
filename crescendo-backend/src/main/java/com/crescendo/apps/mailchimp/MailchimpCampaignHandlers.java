package com.crescendo.apps.mailchimp;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
// import java.util.List;
import java.util.Map;

/**
 * Mailchimp Campaign handlers.
 * Operations (from n8n Mailchimp.node.ts, resource='campaign'):
 *   - create   : POST /3.0/campaigns
 *   - delete   : DELETE /3.0/campaigns/{campaignId}
 *   - get      : GET /3.0/campaigns/{campaignId}
 *   - getAll   : GET /3.0/campaigns
 *   - replicate: POST /3.0/campaigns/{campaignId}/actions/replicate
 *   - resend   : POST /3.0/campaigns/{campaignId}/actions/create-resend
 *   - send     : POST /3.0/campaigns/{campaignId}/actions/send
 */
@Component
public class MailchimpCampaignHandlers {

    private String getDataCenter(String apiKey) {
        // Mailchimp API key format: <key>-us1 — last segment is data center
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

    @ActionMapping(appKey = "mailchimp", actionKey = "mailchimp:campaign:create")
    public Object createCampaign(ActionContext context) throws Exception {
        String type = context.getString("type");
        String listId = context.getString("listId");
        String subject = context.getString("subject");
        String fromName = context.getString("fromName");
        String replyTo = context.getString("replyTo");
        Map<String, Object> additionalSettings = context.getMap("settings");

        Map<String, Object> body = new HashMap<>();
        body.put("type", type);

        Map<String, Object> recipients = new HashMap<>();
        if (listId != null) recipients.put("list_id", listId);
        body.put("recipients", recipients);

        Map<String, Object> settings = new HashMap<>();
        if (subject != null) settings.put("subject_line", subject);
        if (fromName != null) settings.put("from_name", fromName);
        if (replyTo != null) settings.put("reply_to", replyTo);
        if (additionalSettings != null) settings.putAll(additionalSettings);
        body.put("settings", settings);

        return RestClient.builder()
                .url(getBaseUrl(context) + "/campaigns")
                .header("Authorization", getAuth(context))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "mailchimp", actionKey = "mailchimp:campaign:delete")
    public Object deleteCampaign(ActionContext context) throws Exception {
        String campaignId = context.getString("campaignId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/campaigns/" + campaignId)
                .header("Authorization", getAuth(context))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "mailchimp", actionKey = "mailchimp:campaign:get")
    public Object getCampaign(ActionContext context) throws Exception {
        String campaignId = context.getString("campaignId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/campaigns/" + campaignId)
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "mailchimp", actionKey = "mailchimp:campaign:getAll")
    public Object getAllCampaigns(ActionContext context) throws Exception {
        int limit = context.getInt("limit", 100);
        String status = context.getString("status");

        StringBuilder url = new StringBuilder(getBaseUrl(context) + "/campaigns?count=" + limit);
        if (status != null && !status.isBlank()) url.append("&status=").append(status);

        return RestClient.builder()
                .url(url.toString())
                .header("Authorization", getAuth(context))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "mailchimp", actionKey = "mailchimp:campaign:replicate")
    public Object replicateCampaign(ActionContext context) throws Exception {
        String campaignId = context.getString("campaignId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/campaigns/" + campaignId + "/actions/replicate")
                .header("Authorization", getAuth(context))
                .post(Map.of())
                .execute();
    }

    @ActionMapping(appKey = "mailchimp", actionKey = "mailchimp:campaign:resend")
    public Object resendCampaign(ActionContext context) throws Exception {
        String campaignId = context.getString("campaignId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/campaigns/" + campaignId + "/actions/create-resend")
                .header("Authorization", getAuth(context))
                .post(Map.of())
                .execute();
    }

    @ActionMapping(appKey = "mailchimp", actionKey = "mailchimp:campaign:send")
    public Object sendCampaign(ActionContext context) throws Exception {
        String campaignId = context.getString("campaignId");
        return RestClient.builder()
                .url(getBaseUrl(context) + "/campaigns/" + campaignId + "/actions/send")
                .header("Authorization", getAuth(context))
                .post(Map.of())
                .execute();
    }
}
