package com.crescendo.apps.googledrive;

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
 * Fetches Google Drive resources for dynamic dropdowns.
 * Supports: files, folders, drives (shared drives)
 * Uses the same Drive API v3 pattern as Automatisch.
 */
@Component
public class GoogleDriveResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDriveResourceProvider.class);
    private static final String DRIVE_API = "https://www.googleapis.com/drive/v3";

    @Override
    public String appKey() {
        return "google-drive";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("files", "folders", "drives");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String accessToken = credentials.get("accessToken").toString();

        return switch (resourceType) {
            case "files" -> listFiles(accessToken, params.get("folderId"), null);
            case "folders" -> listFiles(accessToken, null, "application/vnd.google-apps.folder");
            case "drives" -> listSharedDrives(accessToken);
            default -> List.of();
        };
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listFiles(String accessToken, String folderId, String mimeTypeFilter) {
        try {
            StringBuilder query = new StringBuilder();
            if (mimeTypeFilter != null) {
                query.append("mimeType='").append(mimeTypeFilter).append("'");
            } else {
                query.append("mimeType!='application/vnd.google-apps.folder'");
            }
            if (folderId != null && !folderId.isBlank()) {
                query.append(" and '").append(folderId).append("' in parents");
            }
            query.append(" and trashed=false");

            List<ResourceOption> results = new ArrayList<>();
            String pageToken = null;

            do {
                String uri = DRIVE_API + "/files?q={q}&pageSize=100&orderBy=createdTime desc&fields=nextPageToken,files(id,name,mimeType)"
                        + (pageToken != null ? "&pageToken=" + pageToken : "");

                Map<String, Object> response = restClient(accessToken)
                        .get()
                        .uri(uri, query.toString())
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});

                List<Map<String, Object>> files = (List<Map<String, Object>>) response.get("files");
                if (files != null) {
                    for (Map<String, Object> file : files) {
                        results.add(new ResourceOption(
                                file.get("id").toString(),
                                file.get("name").toString(),
                                file.get("mimeType") != null ? file.get("mimeType").toString() : null
                        ));
                    }
                }

                pageToken = response.get("nextPageToken") != null ? response.get("nextPageToken").toString() : null;
            } while (pageToken != null);

            return results;
        } catch (Exception e) {
            logger.error("[google-drive] Failed to list files: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listSharedDrives(String accessToken) {
        try {
            List<ResourceOption> results = new ArrayList<>();
            // Always include "My Drive" as the first option
            results.add(new ResourceOption(null, "My Google Drive", "Personal drive"));

            Map<String, Object> response = restClient(accessToken)
                    .get()
                    .uri(DRIVE_API + "/drives?pageSize=100")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            List<Map<String, Object>> drives = (List<Map<String, Object>>) response.get("drives");
            if (drives != null) {
                for (Map<String, Object> drive : drives) {
                    results.add(new ResourceOption(
                            drive.get("id").toString(),
                            drive.get("name").toString(),
                            "Shared drive"
                    ));
                }
            }

            return results;
        } catch (Exception e) {
            logger.error("[google-drive] Failed to list shared drives: {}", e.getMessage());
            return List.of(new ResourceOption(null, "My Google Drive", "Personal drive"));
        }
    }

    private RestClient restClient(String accessToken) {
        return RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
