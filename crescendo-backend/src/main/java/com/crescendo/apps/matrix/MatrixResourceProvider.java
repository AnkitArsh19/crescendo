package com.crescendo.apps.matrix;

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
 * Fetches Matrix resources: rooms the user has joined.
 * Authenticates via Access Token.
 */
@Component
@SuppressWarnings("unchecked")
public class MatrixResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(MatrixResourceProvider.class);

    private final RestClient restClient;

    public MatrixResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "matrix";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("rooms");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of(new ResourceContextDescriptor("rooms", 50, java.time.Duration.ofMinutes(5)));
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials, String resourceType,
            Map<String, String> params) {
        String baseUrl = str(credentials.get("baseUrl")).replaceAll("/+$", "");
        String token = str(credentials.get("accessToken"));
        if (baseUrl.isBlank() || token.isBlank()) {
            throw new IllegalArgumentException("Matrix requires baseUrl and accessToken credentials.");
        }

        return switch (resourceType) {
            case "rooms" -> listRooms(baseUrl, token);
            default -> List.of();
        };
    }

    private List<ResourceOption> listRooms(String baseUrl, String token) {
        try {
            Map<String, Object> resp = restClient.get()
                    .uri(baseUrl + "/_matrix/client/v3/joined_rooms")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (resp == null) return List.of();
            List<String> roomIds = (List<String>) resp.get("joined_rooms");
            if (roomIds == null) return List.of();
            // Return rooms - try to get display name via state
            List<ResourceOption> options = new ArrayList<>();
            for (String roomId : roomIds) {
                String displayName = getRoomDisplayName(baseUrl, token, roomId);
                options.add(new ResourceOption(roomId, displayName, roomId));
            }
            return options;
        } catch (Exception e) {
            logger.error("[matrix] Failed to list rooms: {}", e.getMessage());
            return List.of();
        }
    }

    private String getRoomDisplayName(String baseUrl, String token, String roomId) {
        try {
            Map<String, Object> state = restClient.get()
                    .uri(baseUrl + "/_matrix/client/v3/rooms/" + roomId + "/state/m.room.name/")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(Map.class);
            if (state != null && state.containsKey("name")) {
                return str(state.get("name"));
            }
        } catch (Exception ignored) {
            // Room may not have a name event
        }
        return roomId;
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
