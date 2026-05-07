package com.crescendo.apps.airtable;

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
 * Fetches Airtable resources: bases, tables.
 * Uses the Airtable REST API (with OAuth access tokens).
 */
@Component
public class AirtableResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(AirtableResourceProvider.class);
    private static final String AIRTABLE_API = "https://api.airtable.com/v0";

    @Override
    public String appKey() {
        return "airtable";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("bases", "tables");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String accessToken = credentials.get("accessToken").toString();

        return switch (resourceType) {
            case "bases" -> listBases(accessToken);
            case "tables" -> listTables(accessToken, params.get("baseId"));
            default -> List.of();
        };
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listBases(String accessToken) {
        try {
            Map<String, Object> response = restClient(accessToken)
                    .get()
                    .uri(AIRTABLE_API + "/meta/bases")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> bases = (List<Map<String, Object>>) response.get("bases");
            if (bases == null) return List.of();

            return bases.stream()
                    .map(b -> new ResourceOption(
                            b.get("id").toString(),
                            b.get("name").toString(),
                            b.get("permissionLevel") != null ? b.get("permissionLevel").toString() : null
                    ))
                    .toList();
        } catch (Exception e) {
            logger.error("[airtable] Failed to list bases: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listTables(String accessToken, String baseId) {
        if (baseId == null || baseId.isBlank()) return List.of();

        try {
            Map<String, Object> response = restClient(accessToken)
                    .get()
                    .uri(AIRTABLE_API + "/meta/bases/{baseId}/tables", baseId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> tables = (List<Map<String, Object>>) response.get("tables");
            if (tables == null) return List.of();

            return tables.stream()
                    .map(t -> new ResourceOption(
                            t.get("id").toString(),
                            t.get("name").toString(),
                            t.get("description") != null ? t.get("description").toString() : null
                    ))
                    .toList();
        } catch (Exception e) {
            logger.error("[airtable] Failed to list tables for base {}: {}", baseId, e.getMessage());
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
