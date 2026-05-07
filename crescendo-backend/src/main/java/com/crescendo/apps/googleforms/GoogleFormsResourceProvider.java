package com.crescendo.apps.googleforms;

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
 * Lists Google Forms from the user's Drive.
 * Supports: forms
 */
@Component
public class GoogleFormsResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(GoogleFormsResourceProvider.class);
    private static final String DRIVE_API = "https://www.googleapis.com/drive/v3";

    @Override
    public String appKey() {
        return "google-forms";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("forms");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String accessToken = credentials.get("accessToken").toString();
        return listForms(accessToken);
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listForms(String accessToken) {
        try {
            String query = "mimeType='application/vnd.google-apps.form' and trashed=false";

            Map<String, Object> response = restClient(accessToken)
                    .get()
                    .uri(DRIVE_API + "/files?q={q}&pageSize=100&orderBy=modifiedTime desc&fields=files(id,name,modifiedTime)", query)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> files = (List<Map<String, Object>>) response.get("files");
            if (files == null) return List.of();

            return files.stream()
                    .map(f -> new ResourceOption(
                            f.get("id").toString(),
                            f.get("name").toString(),
                            f.get("modifiedTime") != null ? "Modified: " + f.get("modifiedTime") : null
                    ))
                    .toList();
        } catch (Exception e) {
            logger.error("[google-forms] Failed to list forms: {}", e.getMessage());
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
