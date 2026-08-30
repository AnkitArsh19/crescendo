package com.crescendo.apps.freshdesk;

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
 * Fetches Freshdesk resources: contacts, tickets, agents, groups.
 * Authenticates via API Key (base64 of "apiKey:X").
 */
@Component
@SuppressWarnings("unchecked")
public class FreshdeskResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(FreshdeskResourceProvider.class);

    private final RestClient restClient;

    public FreshdeskResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "freshdesk";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("contacts", "tickets", "agents", "groups");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of(
                new ResourceContextDescriptor("agents", 50, java.time.Duration.ofMinutes(10)),
                new ResourceContextDescriptor("groups", 50, java.time.Duration.ofMinutes(10))
        );
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials, String resourceType,
            Map<String, String> params) {
        String domain = str(credentials.get("domain"));
        String apiKey = str(credentials.get("apiKey"));
        if (domain.isBlank() || apiKey.isBlank()) {
            throw new IllegalArgumentException("Freshdesk requires domain and apiKey credentials.");
        }
        // Freshdesk uses HTTP Basic auth: apiKey as username, "X" as password
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString((apiKey + ":X").getBytes());
        String base = "https://" + domain + "/api/v2";

        return switch (resourceType) {
            case "contacts" -> listContacts(base, basicAuth);
            case "tickets"  -> listTickets(base, basicAuth);
            case "agents"   -> listAgents(base, basicAuth);
            case "groups"   -> listGroups(base, basicAuth);
            default -> List.of();
        };
    }

    private List<ResourceOption> listContacts(String base, String auth) {
        try {
            List<Map<String, Object>> contacts = restClient.get()
                    .uri(base + "/contacts?per_page=100")
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .retrieve().body(List.class);
            if (contacts == null) return List.of();
            return contacts.stream().map(c -> {
                String id = String.valueOf(c.get("id"));
                String name = str(c.get("name"));
                String email = str(c.get("email"));
                return new ResourceOption(id, name.isBlank() ? "Contact " + id : name, email);
            }).toList();
        } catch (Exception e) {
            logger.error("[freshdesk] Failed to list contacts: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listTickets(String base, String auth) {
        try {
            List<Map<String, Object>> tickets = restClient.get()
                    .uri(base + "/tickets?per_page=100&order_by=created_at&order_type=desc")
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .retrieve().body(List.class);
            if (tickets == null) return List.of();
            return tickets.stream().map(t -> {
                String id = String.valueOf(t.get("id"));
                String subject = str(t.get("subject"));
                int status = t.get("status") instanceof Number ? ((Number) t.get("status")).intValue() : 0;
                String statusLabel = switch (status) {
                    case 2 -> "Open";
                    case 3 -> "Pending";
                    case 4 -> "Resolved";
                    case 5 -> "Closed";
                    default -> "Unknown";
                };
                return new ResourceOption(id, "#" + id + " " + (subject.isBlank() ? "Ticket" : subject), statusLabel);
            }).toList();
        } catch (Exception e) {
            logger.error("[freshdesk] Failed to list tickets: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listAgents(String base, String auth) {
        try {
            List<Map<String, Object>> agents = restClient.get()
                    .uri(base + "/agents?per_page=100")
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .retrieve().body(List.class);
            if (agents == null) return List.of();
            return agents.stream().map(a -> {
                String id = String.valueOf(a.get("id"));
                Map<String, Object> contact = (Map<String, Object>) a.getOrDefault("contact", Map.of());
                String name = str(contact.get("name"));
                String email = str(contact.get("email"));
                return new ResourceOption(id, name.isBlank() ? "Agent " + id : name, email);
            }).toList();
        } catch (Exception e) {
            logger.error("[freshdesk] Failed to list agents: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listGroups(String base, String auth) {
        try {
            List<Map<String, Object>> groups = restClient.get()
                    .uri(base + "/groups?per_page=100")
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .retrieve().body(List.class);
            if (groups == null) return List.of();
            return groups.stream().map(g -> {
                String id = String.valueOf(g.get("id"));
                String name = str(g.get("name"));
                String desc = str(g.get("description"));
                return new ResourceOption(id, name.isBlank() ? "Group " + id : name, desc);
            }).toList();
        } catch (Exception e) {
            logger.error("[freshdesk] Failed to list groups: {}", e.getMessage());
            return List.of();
        }
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
