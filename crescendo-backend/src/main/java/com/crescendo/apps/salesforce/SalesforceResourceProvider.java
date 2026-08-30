package com.crescendo.apps.salesforce;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceContextDescriptor;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Fetches Salesforce standard resources: Accounts, Contacts, Leads, Opportunities, Cases, and Campaigns.
 * Authenticates via OAuth2 access token and instanceUrl.
 */
@Component
@SuppressWarnings("unchecked")
public class SalesforceResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(SalesforceResourceProvider.class);
    private static final String API_VERSION = "v58.0";

    private final RestClient restClient;

    public SalesforceResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "salesforce";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("accounts", "contacts", "leads", "opportunities", "cases", "campaigns");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of(
                new ResourceContextDescriptor("accounts", 100, java.time.Duration.ofMinutes(10)),
                new ResourceContextDescriptor("campaigns", 50, java.time.Duration.ofMinutes(10))
        );
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials, String resourceType,
            Map<String, String> params) {
        String token = getToken(credentials);
        String instanceUrl = getInstanceUrl(credentials);

        return switch (resourceType) {
            case "accounts"      -> queryObjects(instanceUrl, token, "SELECT Id, Name FROM Account ORDER BY Name LIMIT 100", "Name", "Account");
            case "contacts"      -> queryObjects(instanceUrl, token, "SELECT Id, Name, Email FROM Contact ORDER BY Name LIMIT 100", "Name", "Email");
            case "leads"         -> queryObjects(instanceUrl, token, "SELECT Id, Name, Company FROM Lead ORDER BY Name LIMIT 100", "Name", "Company");
            case "opportunities"-> queryObjects(instanceUrl, token, "SELECT Id, Name, StageName FROM Opportunity ORDER BY Name LIMIT 100", "Name", "StageName");
            case "cases"         -> queryCases(instanceUrl, token);
            case "campaigns"     -> queryObjects(instanceUrl, token, "SELECT Id, Name FROM Campaign ORDER BY Name LIMIT 100", "Name", "Campaign");
            default -> List.of();
        };
    }

    private List<ResourceOption> queryObjects(String instanceUrl, String token, String soql, String nameField, String subField) {
        try {
            String encoded = URLEncoder.encode(soql, StandardCharsets.UTF_8);
            Map<String, Object> resp = restClient.get()
                    .uri(instanceUrl + "/services/data/" + API_VERSION + "/query?q=" + encoded)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (resp == null || !resp.containsKey("records")) return List.of();
            List<Map<String, Object>> records = (List<Map<String, Object>>) resp.get("records");
            if (records == null) return List.of();

            return records.stream().map(r -> {
                String id = str(r.get("Id"));
                String name = str(r.get(nameField));
                String sub = str(r.get(subField));
                return new ResourceOption(id, name.isBlank() ? "ID: " + id : name, sub);
            }).toList();
        } catch (Exception e) {
            logger.error("[salesforce] Failed to execute SOQL '{}': {}", soql, e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> queryCases(String instanceUrl, String token) {
        try {
            String soql = "SELECT Id, CaseNumber, Subject, Status FROM Case ORDER BY CreatedDate DESC LIMIT 100";
            String encoded = URLEncoder.encode(soql, StandardCharsets.UTF_8);
            Map<String, Object> resp = restClient.get()
                    .uri(instanceUrl + "/services/data/" + API_VERSION + "/query?q=" + encoded)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (resp == null || !resp.containsKey("records")) return List.of();
            List<Map<String, Object>> records = (List<Map<String, Object>>) resp.get("records");
            if (records == null) return List.of();

            return records.stream().map(r -> {
                String id = str(r.get("Id"));
                String num = str(r.get("CaseNumber"));
                String subject = str(r.get("Subject"));
                String status = str(r.get("Status"));
                String label = "#" + num + (subject.isBlank() ? "" : " - " + subject);
                return new ResourceOption(id, label, "Status: " + status);
            }).toList();
        } catch (Exception e) {
            logger.error("[salesforce] Failed to query cases: {}", e.getMessage());
            return List.of();
        }
    }

    private String getToken(Map<String, Object> credentials) {
        if (credentials != null) {
            for (String key : new String[]{"accessToken", "token", "apiKey"}) {
                Object val = credentials.get(key);
                if (val != null && !val.toString().isBlank()) return val.toString();
            }
        }
        throw new IllegalArgumentException("Salesforce requires an access token.");
    }

    private String getInstanceUrl(Map<String, Object> credentials) {
        if (credentials != null) {
            for (String key : new String[]{"instanceUrl", "instance_url", "baseUrl", "domain"}) {
                Object val = credentials.get(key);
                if (val != null && !val.toString().isBlank()) {
                    String url = val.toString().replaceAll("/+$", "");
                    if (!url.startsWith("http")) url = "https://" + url;
                    return url;
                }
            }
        }
        return "https://login.salesforce.com";
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
