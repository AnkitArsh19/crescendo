package com.crescendo.apps.hubspot;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HubSpotCompanyHandlers {

    @ActionMapping(appKey = "hubspot", actionKey = "company-create")
    public Object createCompany(ActionContext context) throws Exception {
        String name = context.getString("name");
        Map<String, Object> properties = Map.of("name", name);
        Map<String, Object> body = Map.of("properties", properties);

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/companies")
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "company-delete")
    public Object deleteCompany(ActionContext context) throws Exception {
        String companyId = context.getString("companyId");

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/companies/" + companyId)
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .delete()
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "company-get")
    public Object getCompany(ActionContext context) throws Exception {
        String companyId = context.getString("companyId");

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/companies/" + companyId)
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "company-getAll")
    public Object getAllCompanies(ActionContext context) throws Exception {
        int limit = context.getInt("limit", 100);

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/companies?limit=" + limit)
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .get()
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "company-getRecentlyCreatedUpdated")
    public Object getRecentlyCreatedUpdatedCompanies(ActionContext context) throws Exception {
        int limit = context.getInt("limit", 100);
        // Note: CRM v3 uses search for recently created/updated
        Map<String, Object> body = Map.of(
            "sorts", java.util.List.of(Map.of("propertyName", "hs_lastmodifieddate", "direction", "DESCENDING")),
            "limit", limit
        );

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/companies/search")
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "company-searchByDomain")
    public Object searchCompanyByDomain(ActionContext context) throws Exception {
        String domain = context.getString("domain");
        Map<String, Object> body = Map.of(
            "filterGroups", java.util.List.of(
                Map.of("filters", java.util.List.of(
                    Map.of("propertyName", "domain", "operator", "EQ", "value", domain)
                ))
            )
        );

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/companies/search")
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "hubspot", actionKey = "company-update")
    public Object updateCompany(ActionContext context) throws Exception {
        String companyId = context.getString("companyId");
        Map<String, Object> properties = context.getMap("properties");
        if (properties == null) {
            properties = Map.of();
        }
        Map<String, Object> body = Map.of("properties", properties);

        return RestClient.builder()
                .url("https://api.hubapi.com/crm/v3/objects/companies/" + companyId)
                .header("Authorization", "Bearer " + context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .patch(body)
                .execute();
    }
}
