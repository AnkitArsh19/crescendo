package com.crescendo.apps.hubspot;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HubSpotDealHandlers {

    @ActionMapping(appKey = "hubspot", actionKey = "deal-create")
    public Object createDeal(ActionContext context) throws Exception {
        Map<String, Object> properties = context.getMap("properties");
        if (properties == null) {
            properties = new java.util.HashMap<>();
        }
        String pipeline = context.getString("pipeline");
        String stage = context.getString("stage");

        if (pipeline != null && !pipeline.isBlank()) {
            properties.put("pipeline", pipeline);
        }
        if (stage != null && !stage.isBlank()) {
            properties.put("dealstage", stage);
        }

        Map<String, Object> body = Map.of("properties", properties);

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/deals")
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "deal-delete")
    public Object deleteDeal(ActionContext context) throws Exception {
        String dealId = context.getString("dealId");

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/deals/" + dealId)
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "deal-get")
    public Object getDeal(ActionContext context) throws Exception {
        String dealId = context.getString("dealId");

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/deals/" + dealId)
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "deal-getAll")
    public Object getAllDeals(ActionContext context) throws Exception {
        int limit = context.getInt("limit", 100);

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/deals?limit=" + limit)
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "deal-getRecentlyCreatedUpdated")
    public Object getRecentlyCreatedUpdatedDeals(ActionContext context) throws Exception {
        int limit = context.getInt("limit", 100);
        Map<String, Object> body = Map.of(
                "sorts", java.util.List.of(Map.of("propertyName", "hs_lastmodifieddate", "direction", "DESCENDING")),
                "limit", limit
        );

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/deals/search")
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "deal-search")
    public Object searchDeals(ActionContext context) throws Exception {
        String query = context.getString("query");
        Map<String, Object> body = Map.of(
                "query", query
        );

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/deals/search")
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "deal-update")
    public Object updateDeal(ActionContext context) throws Exception {
        String dealId = context.getString("dealId");
        Map<String, Object> properties = context.getMap("properties");
        if (properties == null) {
            properties = Map.of();
        }
        Map<String, Object> body = Map.of("properties", properties);

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/deals/" + dealId)
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .patch(body)
                .execute();
    }
}
