package com.crescendo.apps.dropbox;

import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
public class DropboxResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(DropboxResourceProvider.class);
    private static final String BASE_URL = "https://api.dropboxapi.com/2";

    private static final List<ResourceOption> DEFAULT_FOLDERS = List.of(
            new ResourceOption("", "/ (Root Directory)"),
            new ResourceOption("/Documents", "/Documents"),
            new ResourceOption("/Photos", "/Photos"),
            new ResourceOption("/Uploads", "/Uploads")
    );

    @Override
    public String appKey() {
        return "dropbox";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("folders");
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        String token = extractToken(credentials);
        if (token == null || token.isBlank()) {
            return DEFAULT_FOLDERS;
        }

        if ("folders".equals(resourceType)) {
            return listFolders(token, params != null ? params.get("path") : null);
        }
        return List.of();
    }

    private String extractToken(Map<String, Object> credentials) {
        if (credentials == null) return null;
        if (credentials.get("accessToken") != null) return credentials.get("accessToken").toString();
        if (credentials.get("token") != null) return credentials.get("token").toString();
        if (credentials.get("apiKey") != null) return credentials.get("apiKey").toString();
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<ResourceOption> listFolders(String token, String folderPath) {
        try {
            RestClient client = RestClient.builder()
                    .baseUrl(BASE_URL)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            String targetPath = (folderPath == null || folderPath.equals("/")) ? "" : folderPath;
            Map<String, Object> reqBody = Map.of("path", targetPath, "recursive", false);

            Map<String, Object> resp = client.post()
                    .uri("/files/list_folder")
                    .body(reqBody)
                    .retrieve()
                    .body(Map.class);

            if (resp != null && resp.get("entries") instanceof List<?> entries) {
                List<ResourceOption> options = new ArrayList<>();
                options.add(new ResourceOption("", "/ (Root Directory)"));
                for (Object itemObj : entries) {
                    if (itemObj instanceof Map<?, ?> item) {
                        String tag = String.valueOf(item.get(".tag"));
                        if ("folder".equalsIgnoreCase(tag)) {
                            String pathDisplay = String.valueOf(item.get("path_display"));
                            String name = String.valueOf(item.get("name"));
                            options.add(new ResourceOption(pathDisplay, name + " (" + pathDisplay + ")"));
                        }
                    }
                }
                return options;
            }
        } catch (Exception e) {
            logger.warn("[dropbox] Error fetching live folders, returning default: {}", e.getMessage());
        }
        return DEFAULT_FOLDERS;
    }
}
