package com.crescendo.apps.googledocs;

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
 * Lists Google Docs from the user's Drive (uses Drive API filtered to docs mimeType).
 * Supports: documents, folders
 */
@Component
public class GoogleDocsResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDocsResourceProvider.class);
    private static final String DRIVE_API = "https://www.googleapis.com/drive/v3";

    @Override
    public String appKey() {
        return "google-docs";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("documents", "folders");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String accessToken = credentials.get("accessToken").toString();
        return switch (resourceType) {
            case "documents" -> listDocuments(accessToken);
            case "folders" -> listFolders(accessToken);
            default -> List.of();
        };
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listDocuments(String accessToken) {
        try {
            String query = "mimeType='application/vnd.google-apps.document' and trashed=false";

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
            logger.error("[google-docs] Failed to list documents: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listFolders(String accessToken) {
        try {
            String query = "mimeType='application/vnd.google-apps.folder' and trashed=false";
            Map<String, Object> response = restClient(accessToken)
                    .get()
                    .uri(DRIVE_API + "/files?q={q}&pageSize=100&orderBy=name&fields=files(id,name)", query)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            List<Map<String, Object>> files = response != null
                    ? (List<Map<String, Object>>) response.get("files")
                    : List.of();
            if (files == null) return List.of();
            return files.stream()
                    .map(file -> new ResourceOption(String.valueOf(file.get("id")),
                            String.valueOf(file.get("name")), "Google Drive folder"))
                    .toList();
        } catch (Exception exception) {
            logger.error("[google-docs] Failed to list folders: {}", exception.getMessage());
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
