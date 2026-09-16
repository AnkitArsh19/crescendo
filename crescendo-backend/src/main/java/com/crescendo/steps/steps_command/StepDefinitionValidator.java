package com.crescendo.steps.steps_command;

import com.crescendo.app.AppService;
import com.crescendo.app.AppDto;
import com.crescendo.connections.connections_command.Connections_commandRepository;
import com.crescendo.enums.StepType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

/**
 * Validates the structural and security integrity of a step definition before it is saved.
 *
 * <p>This validator ensures that:
 * <ul>
 *   <li>The appKey actually exists in the app registry.</li>
 *   <li>The actionKey/triggerKey is valid for the specified appKey.</li>
 *   <li>If a connection is provided, the user actually owns that connection
 *       and the connection is intended for the requested appKey.</li>
 * </ul>
 *
 * <p>This is a critical security boundary. It prevents malicious users from
 * forging a payload to execute actions against another user's connection ID.
 */
@Component
public class StepDefinitionValidator {

    private final AppService appService;
    private final Connections_commandRepository connectionsRepo;

    public StepDefinitionValidator(AppService appService, Connections_commandRepository connectionsRepo) {
        this.appService = appService;
        this.connectionsRepo = connectionsRepo;
    }

    public static String normalizeAppKey(String appKey) {
        if (appKey == null) return null;
        if ("crescendo-mail".equalsIgnoreCase(appKey)) return "crescendomail";
        return appKey;
    }

    public static String normalizeActionKey(String appKey, String actionKey) {
        if (actionKey == null) return null;
        String normalizedApp = normalizeAppKey(appKey);
        if ("leetcode".equalsIgnoreCase(normalizedApp) && "daily-problem".equalsIgnoreCase(actionKey)) return "get-daily-problem";
        if ("hackernews".equalsIgnoreCase(normalizedApp) && "top-stories".equalsIgnoreCase(actionKey)) return "get-top-stories";
        if ("notion".equalsIgnoreCase(normalizedApp) && "append-block".equalsIgnoreCase(actionKey)) return "notion:block:append";
        if ("linear".equalsIgnoreCase(normalizedApp) && "create-issue".equalsIgnoreCase(actionKey)) return "linear:issue:create";
        if ("crescendomail".equalsIgnoreCase(normalizedApp) && "sendEmail".equalsIgnoreCase(actionKey)) return "send";
        if ("agent".equalsIgnoreCase(normalizedApp) && "ai_agent".equalsIgnoreCase(actionKey)) return "agent:ai_agent";
        return actionKey;
    }

    public void validateStepDefinition(UUID userId, StepType type, String appKey, String actionKey, UUID connectionId, Map<String, Object> configuration) {
        if (appKey == null || appKey.isBlank()) return;

        String resolvedAppKey = normalizeAppKey(appKey);
        String resolvedActionKey = normalizeActionKey(resolvedAppKey, actionKey);

        AppDto.AppDetailResponse appDef;
        try {
            appDef = appService.getApp(resolvedAppKey);
        } catch (ResponseStatusException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid appKey: " + appKey);
        }

        if (resolvedActionKey == null || resolvedActionKey.isBlank()) return;

        if (type == StepType.TRIGGER) {
            boolean validTrigger = appDef.triggers().stream()
                    .anyMatch(t -> resolvedActionKey.equals(t.get("triggerKey")));
            if (!validTrigger) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid trigger key for app: " + actionKey);
            }
        } else if (type == StepType.ACTION) {
            boolean validAction = appDef.actions().stream()
                    .anyMatch(a -> resolvedActionKey.equals(a.get("actionKey")))
                    || ("agent".equals(resolvedAppKey) && ("agent:ai_agent".equals(resolvedActionKey) || "ai_agent".equals(resolvedActionKey)));
            if (!validAction) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid action key for app: " + actionKey);
            }
        }

        if (connectionId != null) {
            if (userId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Guest workflows cannot use authenticated connections");
            }
            // Verify the user owns the connection and it matches the appKey (or is an AI provider for agent nodes)
            connectionsRepo.findByIdAndUser_Id(connectionId, userId)
                    .filter(c -> {
                        if ("agent".equals(appKey)) {
                            return java.util.Set.of("agent", "gemini", "openai", "groq", "anthropic", "openrouter").contains(c.getAppKey());
                        }
                        return c.getAppKey().equals(appKey);
                    })
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or unauthorized connection for this app"));
        }
    }
}
