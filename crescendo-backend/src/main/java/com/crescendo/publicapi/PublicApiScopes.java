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
    public static final String DOMAIN_READ = "domain:read";
    public static final String DOMAIN_WRITE = "domain:write";
    public static final String CONTACT_READ = "contact:read";
    public static final String CONTACT_WRITE = "contact:write";
    public static final String SUPPRESSION_READ = "suppression:read";
    public static final String SUPPRESSION_WRITE = "suppression:write";
    public static final String SUPPRESSION_IMPORT = "suppression:import";
    public static final String TEMPLATE_READ = "template:read";
    public static final String TEMPLATE_WRITE = "template:write";
    public static final String WEBHOOK_READ = "webhook:read";
    public static final String WEBHOOK_WRITE = "webhook:write";
    public static final String LOGS_READ = "logs:read";
    public static final String METRICS_READ = "metrics:read";
    public static final String CUSTOM_EVENT_READ  = "customevent:read";
    public static final String CUSTOM_EVENT_WRITE = "customevent:write";


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
            AI_BUILD,
            DOMAIN_READ,
            DOMAIN_WRITE,
            CONTACT_READ,
            CONTACT_WRITE,
            SUPPRESSION_READ,
            SUPPRESSION_WRITE,
            SUPPRESSION_IMPORT,
            TEMPLATE_READ,
            TEMPLATE_WRITE,
            WEBHOOK_READ,
            WEBHOOK_WRITE,
            LOGS_READ,
            METRICS_READ,
            CUSTOM_EVENT_READ,
            CUSTOM_EVENT_WRITE
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
