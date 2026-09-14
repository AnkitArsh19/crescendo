package com.crescendo.apps.telegram.controller;

import com.crescendo.apps.telegram.service.TelegramLinkService;
import com.crescendo.security.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/telegram/link")
public class TelegramLinkController {

    private final TelegramLinkService linkService;

    public TelegramLinkController(TelegramLinkService linkService) {
        this.linkService = linkService;
    }

    /**
     * Initiates a Telegram account linking flow for the authenticated Crescendo user.
     * Returns a 10-minute deep-link (https://t.me/bot?start=token) and nonce token.
     */
    @PostMapping("/initiate")
    public ResponseEntity<Map<String, Object>> initiateLink(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails details)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to link Telegram");
        }

        UUID userId = details.getId();
        Map<String, Object> response = linkService.initiateLink(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Polls the status of an in-flight linking request.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getLinkStatus(@RequestParam String token) {
        Map<String, Object> status = linkService.getLinkStatus(token);
        return ResponseEntity.ok(status);
    }

    /**
     * Directly adds a Telegram channel or group by username (@mychannel) or chat ID (-100...).
     */
    @PostMapping("/add-chat")
    public ResponseEntity<Map<String, Object>> addChat(Authentication auth, @RequestBody Map<String, String> body) {
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails details)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to add chat");
        }

        String chatId = body != null ? body.get("chatId") : null;
        if (chatId == null || chatId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'chatId' is required (e.g. @channel or chat ID)");
        }

        com.crescendo.apps.telegram.entity.UserTelegramChat chat = linkService.addChatDirectly(details.getId(), chatId.trim());
        String typeDesc = switch (chat.getType().toLowerCase()) {
            case "channel" -> "Channel";
            case "group" -> "Group";
            case "supergroup" -> "Supergroup";
            case "private" -> "Direct Chat";
            default -> chat.getType();
        };

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "id", chat.getChatId(),
                "label", chat.getTitle(),
                "description", typeDesc + " · ID: " + chat.getChatId()
        ));
    }
}
