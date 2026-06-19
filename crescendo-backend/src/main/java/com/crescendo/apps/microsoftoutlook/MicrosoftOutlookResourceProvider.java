package com.crescendo.apps.microsoftoutlook;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Lists Outlook mail folders and contacts via Microsoft Graph API.
 * Supports: mailFolders, contacts
 */
@Component
@SuppressWarnings("unchecked")
public class MicrosoftOutlookResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(MicrosoftOutlookResourceProvider.class);
    private static final String GRAPH_API = "https://graph.microsoft.com/v1.0";

    @Override
    public String appKey() {
        return "microsoft-outlook";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("mailFolders", "contacts", "calendars");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String accessToken = credentials.get("accessToken").toString();

        return switch (resourceType) {
            case "mailFolders" -> listMailFolders(accessToken);
            case "contacts" -> listContacts(accessToken);
            case "calendars" -> listCalendars(accessToken);
            default -> List.of();
        };
    }

    private List<ResourceOption> listMailFolders(String accessToken) {
        try {
            Map<String, Object> response = restClient(accessToken)
                    .get()
                    .uri(GRAPH_API + "/me/mailFolders?top=100")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> folders = (List<Map<String, Object>>) response.get("value");
            if (folders == null) return List.of();

            return folders.stream()
                    .map(folder -> new ResourceOption(
                            folder.get("id").toString(),
                            folder.get("displayName").toString(),
                            folder.get("totalItemCount") != null ? folder.get("totalItemCount") + " items" : null
                    ))
                    .toList();
        } catch (Exception e) {
            logger.error("[microsoft-outlook] Failed to list mail folders: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listContacts(String accessToken) {
        try {
            Map<String, Object> response = restClient(accessToken)
                    .get()
                    .uri(GRAPH_API + "/me/contacts?top=100&select=id,displayName,emailAddresses")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> contacts = (List<Map<String, Object>>) response.get("value");
            if (contacts == null) return List.of();

            return contacts.stream()
                    .map(c -> {
                        String email = null;
                        Object emails = c.get("emailAddresses");
                        if (emails instanceof List<?> emailList && !emailList.isEmpty()) {
                            Object first = emailList.get(0);
                            if (first instanceof Map<?, ?> emailMap) {
                                email = emailMap.get("address") != null ? emailMap.get("address").toString() : null;
                            }
                        }
                        return new ResourceOption(
                                c.get("id").toString(),
                                c.get("displayName") != null ? c.get("displayName").toString() : "(No name)",
                                email
                        );
                    })
                    .toList();
        } catch (Exception e) {
            logger.error("[microsoft-outlook] Failed to list contacts: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listCalendars(String accessToken) {
        try {
            Map<String, Object> response = restClient(accessToken)
                    .get()
                    .uri(GRAPH_API + "/me/calendars?$select=id,name,owner&$top=50")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> calendars = (List<Map<String, Object>>) response.get("value");
            if (calendars == null) return List.of();

            return calendars.stream()
                    .map(cal -> {
                        String ownerName = null;
                        if (cal.get("owner") instanceof Map<?, ?> owner) {
                            ownerName = owner.get("name") != null ? owner.get("name").toString() : null;
                        }
                        return new ResourceOption(
                                cal.get("id").toString(),
                                cal.get("name").toString(),
                                ownerName != null ? "Owner: " + ownerName : null
                        );
                    })
                    .toList();
        } catch (Exception e) {
            logger.error("[microsoft-outlook] Failed to list calendars: {}", e.getMessage());
            return List.of();
        }
    }

    private RestClient restClient(String accessToken) {
        return RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
