package com.crescendo.apps.hubspot;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HubSpotContactListHandlers {

    @ActionMapping(appKey = "hubspot", actionKey = "contactList-add")
    public Object addContactToList(ActionContext context) throws Exception {
        String listId = context.getString("listId");
        String emails = context.getString("emails");
        java.util.List<String> emailList = emails != null && !emails.isBlank() ? 
                java.util.List.of(emails.split(",")) : java.util.List.of();

        Map<String, Object> body = Map.of("emails", emailList);

        return RestClient.builder()
                .url("https://api.hubapi.com/contacts/v1/lists/" + listId + "/add")
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "contactList-remove")
    public Object removeContactFromList(ActionContext context) throws Exception {
        String listId = context.getString("listId");
        String emails = context.getString("emails");
        java.util.List<String> emailList = emails != null && !emails.isBlank() ? 
                java.util.List.of(emails.split(",")) : java.util.List.of();

        Map<String, Object> body = Map.of("emails", emailList);

        return RestClient.builder()
                .url("https://api.hubapi.com/contacts/v1/lists/" + listId + "/remove")
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }
}
