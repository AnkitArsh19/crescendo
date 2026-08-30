package com.crescendo.apps.pushbullet;

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
 * Fetches Pushbullet resources: devices and chats.
 * Authenticates via Access Token.
 */
@Component
@SuppressWarnings("unchecked")
public class PushbulletResourceProvider implements ResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(PushbulletResourceProvider.class);
    private static final String PUSHBULLET_API = "https://api.pushbullet.com/v2";

    private final RestClient restClient;

    public PushbulletResourceProvider() {
        this.restClient = RestClient.create();
    }

    @Override
    public String appKey() {
        return "pushbullet";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("devices", "chats");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of(new ResourceContextDescriptor("devices", 50, java.time.Duration.ofMinutes(10)));
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials, String resourceType,
            Map<String, String> params) {
        String token = getToken(credentials);
        return switch (resourceType) {
            case "devices" -> listDevices(token);
            case "chats"   -> listChats(token);
            default -> List.of();
        };
    }

    private List<ResourceOption> listDevices(String token) {
        try {
            Map<String, Object> resp = restClient.get()
                    .uri(PUSHBULLET_API + "/devices")
                    .header("Access-Token", token)
                    .retrieve().body(Map.class);
            if (resp == null || !resp.containsKey("devices")) return List.of();
            List<Map<String, Object>> devices = (List<Map<String, Object>>) resp.get("devices");
            if (devices == null) return List.of();

            return devices.stream()
                    .filter(d -> Boolean.TRUE.equals(d.get("active")))
                    .map(d -> {
                        String iden = str(d.get("iden"));
                        String nickname = str(d.get("nickname"));
                        String model = str(d.get("model"));
                        String label = !nickname.isBlank() ? nickname : (!model.isBlank() ? model : "Device " + iden);
                        return new ResourceOption(iden, label, model);
                    }).toList();
        } catch (Exception e) {
            logger.error("[pushbullet] Failed to list devices: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ResourceOption> listChats(String token) {
        try {
            Map<String, Object> resp = restClient.get()
                    .uri(PUSHBULLET_API + "/chats")
                    .header("Access-Token", token)
                    .retrieve().body(Map.class);
            if (resp == null || !resp.containsKey("chats")) return List.of();
            List<Map<String, Object>> chats = (List<Map<String, Object>>) resp.get("chats");
            if (chats == null) return List.of();

            return chats.stream()
                    .filter(c -> Boolean.TRUE.equals(c.get("active")))
                    .map(c -> {
                        String iden = str(c.get("iden"));
                        Map<String, Object> with = (Map<String, Object>) c.getOrDefault("with", Map.of());
                        String name = str(with.get("name"));
                        String email = str(with.get("email"));
                        return new ResourceOption(iden, name.isBlank() ? email : name, email);
                    }).toList();
        } catch (Exception e) {
            logger.error("[pushbullet] Failed to list chats: {}", e.getMessage());
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
        throw new IllegalArgumentException("Pushbullet requires an access token.");
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
