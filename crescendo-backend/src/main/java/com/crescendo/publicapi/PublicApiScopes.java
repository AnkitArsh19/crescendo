package com.crescendo.publicapi;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

public final class PublicApiScopes {
    private PublicApiScopes() {}

    public static final String WORKFLOW_READ = "workflow:read";
    public static final String WORKFLOW_WRITE = "workflow:write";
    public static final String WORKFLOW_TRIGGER = "workflow:trigger";
    public static final String RUN_READ = "run:read";
    public static final String RUN_CANCEL = "run:cancel";
    public static final String CONNECTION_READ = "connection:read";
    public static final String CONNECTION_WRITE = "connection:write";
    public static final String EMAIL_SEND = "email:send";
    public static final String APP_READ = "app:read";
    public static final String AI_BUILD = "ai:build";

    public static final Set<String> ALL = Set.of(
            WORKFLOW_READ,
            WORKFLOW_WRITE,
            WORKFLOW_TRIGGER,
            RUN_READ,
            RUN_CANCEL,
            CONNECTION_READ,
            CONNECTION_WRITE,
            EMAIL_SEND,
            APP_READ,
            AI_BUILD
    );

    public static final List<String> DEFAULT_SCOPES = List.of(
            WORKFLOW_READ,
            WORKFLOW_TRIGGER,
            RUN_READ,
            EMAIL_SEND,
            APP_READ
    );

    public static List<String> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.copyOf(ALL);
        }
        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    }

    public static String serialize(List<String> requestedScopes) {
        List<String> scopes = requestedScopes == null || requestedScopes.isEmpty()
                ? DEFAULT_SCOPES
                : requestedScopes.stream()
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .distinct()
                        .toList();

        for (String scope : scopes) {
            if (!ALL.contains(scope)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown API key scope: " + scope);
            }
        }
        return String.join(",", scopes);
    }

    public static void require(Authentication auth, String scope) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        boolean allowed = auth.getAuthorities().stream()
                .anyMatch(a -> ("SCOPE_" + scope).equals(a.getAuthority()));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Authentication is missing required scope: " + scope);
        }
    }
}
