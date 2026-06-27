package com.crescendo.apps.hubspot;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class HubSpotContactHandlers {

    @ActionMapping(appKey = "hubspot", actionKey = "contact-upsert")
    public Object upsertContact(ActionContext context) throws Exception {
        String email = context.getString("email");
        Map<String, Object> properties = context.getMap("properties");
        
        Map<String, Object> finalProperties = new HashMap<>();
        if (properties != null) {
            finalProperties.putAll(properties);
        }
        finalProperties.put("email", email);

        Map<String, Object> body = Map.of("properties", finalProperties);

        // HubSpot CRM v3 doesn't have a direct upsert endpoint for contacts based on email in the objects API,
        // but typically upsert is done via ID or email. A common approach is trying to create, and if it fails with 409, extract ID and update.
        // For simplicity and matching typical REST wrapper logic, we try search first or direct create.
        // Since we are mirroring n8n's logic, we should probably just use the v1 contact upsert by email if v3 doesn't support it directly, 
        // or search & update. We'll use search then update/create.
        try {
            return RestClient.builder()
                    .url("https://api.hubapi.com/crm/v3/objects/contacts")
                    .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                    .header("Content-Type", "application/json")
                    .post(body)
                    .execute();
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("409")) {
                // Conflict, so we search and update
                Map<String, Object> searchBody = Map.of(
                        "filterGroups", java.util.List.of(
                                Map.of("filters", java.util.List.of(
                                        Map.of("propertyName", "email", "operator", "EQ", "value", email)
                                ))
                        )
                );
        @SuppressWarnings("unchecked")
                Map<String, Object> searchRes = (Map<String, Object>) RestClient.builder()
                        .url("https://api.hubapi.com/crm/v3/objects/contacts/search")
                        .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                        .header("Content-Type", "application/json")
                        .post(searchBody)
                        .execute();
                        
        @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> results = (java.util.List<Map<String, Object>>) searchRes.get("results");
                if (results != null && !results.isEmpty()) {
                    String contactId = String.valueOf(results.get(0).get("id"));
                    return RestClient.builder()
                            .url("https://api.hubapi.com/crm/v3/objects/contacts/" + contactId)
                            .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                            .header("Content-Type", "application/json")
                            .patch(body)
                            .execute();
                }
            }
            throw e;
        }
    }

    @ActionMapping(appKey = "hubspot", actionKey = "contact-delete")
    public Object deleteContact(ActionContext context) throws Exception {
        String contactId = context.getString("contactId");

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/contacts/" + contactId)
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "contact-get")
    public Object getContact(ActionContext context) throws Exception {
        String contactId = context.getString("contactId");

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/contacts/" + contactId)
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "contact-getAll")
    public Object getAllContacts(ActionContext context) throws Exception {
        int limit = context.getInt("limit", 100);

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/contacts?limit=" + limit)
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "contact-getRecentlyCreatedUpdated")
    public Object getRecentlyCreatedUpdatedContacts(ActionContext context) throws Exception {
        int limit = context.getInt("limit", 100);
        Map<String, Object> body = Map.of(
                "sorts", java.util.List.of(Map.of("propertyName", "lastmodifieddate", "direction", "DESCENDING")),
                "limit", limit
        );

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/contacts/search")
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "contact-search")
    public Object searchContacts(ActionContext context) throws Exception {
        String query = context.getString("query");
        Map<String, Object> body = Map.of(
                "query", query
        );

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/contacts/search")
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }
}
