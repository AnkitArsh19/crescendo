package com.crescendo.apps.hubspot;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HubSpotEngagementHandlers {

    @ActionMapping(appKey = "hubspot", actionKey = "engagement-create")
    public Object createEngagement(ActionContext context) throws Exception {
        String type = context.getString("type");
        Map<String, Object> metadata = context.getMap("metadata");
        if (metadata == null) metadata = Map.of();

        Map<String, Object> engagement = Map.of("type", type);
        Map<String, Object> body = Map.of(
                "engagement", engagement,
                "metadata", metadata
        );

        return RestClient.builder()
                .url("https://api.hubapi.com/engagements/v1/engagements")
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "engagement-delete")
    public Object deleteEngagement(ActionContext context) throws Exception {
        String engagementId = context.getString("engagementId");

        return RestClient.builder()
                .url("https://api.hubapi.com/engagements/v1/engagements/" + engagementId)
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "engagement-get")
    public Object getEngagement(ActionContext context) throws Exception {
        String engagementId = context.getString("engagementId");

        return RestClient.builder()
                .url("https://api.hubapi.com/engagements/v1/engagements/" + engagementId)
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "engagement-getAll")
    public Object getAllEngagements(ActionContext context) throws Exception {
        int limit = context.getInt("limit", 100);

        return RestClient.builder()
                .url("https://api.hubapi.com/engagements/v1/engagements/paged?limit=" + limit)
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .get()
                .execute();
    }
}
