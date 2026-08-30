package com.crescendo.apps.hubspot;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceContextDescriptor;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Fetches HubSpot resources: contacts, companies, deals, forms, lists, pipelines, owners.
 * Authenticates via a private app access token (Bearer).
 */
@Component
@SuppressWarnings("unchecked")
public class HubSpotResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(HubSpotResourceProvider.class);
    private static final String HS_API = "https://api.hubapi.com";

    private final RestClient restClient;

    public HubSpotResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "hubspot";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("contacts", "companies", "deals", "forms", "lists", "pipelines", "owners", "tickets");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of(
                new ResourceContextDescriptor("pipelines", 50, java.time.Duration.ofMinutes(10)),
                new ResourceContextDescriptor("owners", 50, java.time.Duration.ofMinutes(10))
        );
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials, String resourceType,
            Map<String, String> params) {
        String token = getToken(credentials);
        return switch (resourceType) {
            case "contacts"  -> listContacts(token);
            case "companies" -> listCompanies(token);
            case "deals"     -> listDeals(token);
            case "forms"     -> listForms(token);
            case "lists"     -> listContactLists(token);
            case "pipelines" -> listPipelines(token);
            case "owners"    -> listOwners(token);
            case "tickets"   -> listTickets(token);
            default          -> List.of();
        };
    }

    private List<ResourceOption> listContacts(String token) {
        try {
            Map<String, Object> resp = restClient.get()
                    .uri(HS_API + "/crm/v3/objects/contacts?limit=100&properties=firstname,lastname,email")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (resp == null) return List.of();
            List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
            if (results == null) return List.of();
            return results.stream().map(c -> {
                String id = String.valueOf(c.get("id"));
                Map<String, Object> props = (Map<String, Object>) c.getOrDefault("properties", Map.of());
                String fn = str(props.get("firstname"));
                String ln = str(props.get("lastname"));
                String email = str(props.get("email"));
                String name = (fn + " " + ln).trim();
                if (name.isBlank()) name = email;
                return new ResourceOption(id, name.isBlank() ? "Contact " + id : name, email);
            }).toList();
        } catch (Exception e) {
            logger.error("[hubspot] Failed to list contacts: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listCompanies(String token) {
        try {
            Map<String, Object> resp = restClient.get()
                    .uri(HS_API + "/crm/v3/objects/companies?limit=100&properties=name,domain")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (resp == null) return List.of();
            List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
            if (results == null) return List.of();
            return results.stream().map(c -> {
                String id = String.valueOf(c.get("id"));
                Map<String, Object> props = (Map<String, Object>) c.getOrDefault("properties", Map.of());
                String name = str(props.get("name"));
                String domain = str(props.get("domain"));
                return new ResourceOption(id, name.isBlank() ? "Company " + id : name, domain);
            }).toList();
        } catch (Exception e) {
            logger.error("[hubspot] Failed to list companies: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listDeals(String token) {
        try {
            Map<String, Object> resp = restClient.get()
                    .uri(HS_API + "/crm/v3/objects/deals?limit=100&properties=dealname,amount,dealstage")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (resp == null) return List.of();
            List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
            if (results == null) return List.of();
            return results.stream().map(d -> {
                String id = String.valueOf(d.get("id"));
                Map<String, Object> props = (Map<String, Object>) d.getOrDefault("properties", Map.of());
                String name = str(props.get("dealname"));
                String stage = str(props.get("dealstage"));
                return new ResourceOption(id, name.isBlank() ? "Deal " + id : name, "Stage: " + stage);
            }).toList();
        } catch (Exception e) {
            logger.error("[hubspot] Failed to list deals: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listForms(String token) {
        try {
            List<Map<String, Object>> forms = restClient.get()
                    .uri(HS_API + "/marketing/v3/forms?limit=50")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(List.class);
            if (forms == null) return List.of();
            return forms.stream().map(f -> {
                String id = str(f.get("id"));
                String name = str(f.get("name"));
                return new ResourceOption(id, name.isBlank() ? "Form " + id : name, "HubSpot Form");
            }).toList();
        } catch (Exception e) {
            // Try v2 fallback
            try {
                List<Map<String, Object>> forms = restClient.get()
                        .uri(HS_API + "/forms/v2/forms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve().body(List.class);
                if (forms == null) return List.of();
                return forms.stream().map(f -> {
                    String id = str(f.get("guid"));
                    String name = str(f.get("name"));
                    return new ResourceOption(id, name.isBlank() ? "Form " + id : name, "HubSpot Form");
                }).toList();
            } catch (Exception e2) {
                logger.error("[hubspot] Failed to list forms: {}", e2.getMessage());
                return List.of();
            }
        }
    }

    private List<ResourceOption> listContactLists(String token) {
        try {
            Map<String, Object> resp = restClient.get()
                    .uri(HS_API + "/contacts/v1/lists?count=100")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (resp == null) return List.of();
            List<Map<String, Object>> lists = (List<Map<String, Object>>) resp.get("lists");
            if (lists == null) return List.of();
            return lists.stream().map(l -> {
                String id = String.valueOf(l.get("listId"));
                String name = str(l.get("name"));
                Boolean dynamic = (Boolean) l.getOrDefault("dynamic", false);
                return new ResourceOption(id, name.isBlank() ? "List " + id : name,
                        dynamic ? "Dynamic List" : "Static List");
            }).toList();
        } catch (Exception e) {
            logger.error("[hubspot] Failed to list contact lists: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listPipelines(String token) {
        try {
            Map<String, Object> resp = restClient.get()
                    .uri(HS_API + "/crm/v3/pipelines/deals")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (resp == null) return List.of();
            List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
            if (results == null) return List.of();
            return results.stream().map(p -> {
                String id = str(p.get("id"));
                String label = str(p.get("label"));
                return new ResourceOption(id, label.isBlank() ? "Pipeline " + id : label, "Deal Pipeline");
            }).toList();
        } catch (Exception e) {
            logger.error("[hubspot] Failed to list pipelines: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listOwners(String token) {
        try {
            List<Map<String, Object>> owners = restClient.get()
                    .uri(HS_API + "/crm/v3/owners?limit=100")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(List.class);
            if (owners == null) return List.of();
            return owners.stream().map(o -> {
                String id = String.valueOf(o.get("id"));
                String fn = str(o.get("firstName"));
                String ln = str(o.get("lastName"));
                String email = str(o.get("email"));
                String name = (fn + " " + ln).trim();
                if (name.isBlank()) name = email;
                return new ResourceOption(id, name.isBlank() ? "Owner " + id : name, email);
            }).toList();
        } catch (Exception e) {
            logger.error("[hubspot] Failed to list owners: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listTickets(String token) {
        try {
            Map<String, Object> resp = restClient.get()
                    .uri(HS_API + "/crm/v3/objects/tickets?limit=100&properties=subject,hs_pipeline_stage")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (resp == null) return List.of();
            List<Map<String, Object>> results = (List<Map<String, Object>>) resp.get("results");
            if (results == null) return List.of();
            return results.stream().map(t -> {
                String id = String.valueOf(t.get("id"));
                Map<String, Object> props = (Map<String, Object>) t.getOrDefault("properties", Map.of());
                String subject = str(props.get("subject"));
                String stage = str(props.get("hs_pipeline_stage"));
                return new ResourceOption(id, subject.isBlank() ? "Ticket " + id : subject, "Stage: " + stage);
            }).toList();
        } catch (Exception e) {
            logger.error("[hubspot] Failed to list tickets: {}", e.getMessage());
            return List.of();
        }
    }

    private String getToken(Map<String, Object> credentials) {
        if (credentials != null) {
            for (String key : new String[]{"apiKey", "accessToken", "token"}) {
                Object val = credentials.get(key);
                if (val != null && !val.toString().isBlank()) return val.toString();
            }
        }
        throw new IllegalArgumentException("HubSpot connection requires an API Key / Access Token.");
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
