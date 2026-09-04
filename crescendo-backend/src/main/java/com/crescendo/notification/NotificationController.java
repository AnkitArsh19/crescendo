package com.crescendo.notification;

import com.crescendo.notification.preference.NotificationPreferenceDto;
import com.crescendo.notification.workflow.WorkflowNotificationSetting;
import com.crescendo.notification.workflow.WorkflowNotificationSetting.WorkflowNotifyMode;
import com.crescendo.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final UserNotificationService notificationService;
    private final NotificationSseService sseService;

    public NotificationController(
            UserNotificationService notificationService,
            NotificationSseService sseService) {
        this.notificationService = notificationService;
        this.sseService = sseService;
    }

    @GetMapping
    public ResponseEntity<Page<UserNotificationDto>> getNotifications(
            @RequestParam(name = "filter", defaultValue = "all") String filter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Authentication authentication) {
        UUID userId = AuthenticatedUser.userId(authentication);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        return ResponseEntity.ok(notificationService.getNotifications(userId, filter, pageable));
    }

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(Authentication authentication) {
        UUID userId = AuthenticatedUser.userId(authentication);
        SseEmitter emitter = sseService.connect(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform")
                .header(HttpHeaders.CONNECTION, "keep-alive")
                .header("X-Accel-Buffering", "no")
                .body(emitter);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        UUID userId = AuthenticatedUser.userId(authentication);
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(userId)));
    }

    public record MarkReadRequest(@NotEmpty List<UUID> ids) {}

    @PostMapping("/mark-read")
    public ResponseEntity<Map<String, Integer>> markRead(
            @Valid @RequestBody MarkReadRequest request,
            Authentication authentication) {
        UUID userId = AuthenticatedUser.userId(authentication);
        int updated = notificationService.markRead(userId, request.ids());
        return ResponseEntity.ok(Map.of("marked", updated));
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Map<String, Integer>> markAllRead(Authentication authentication) {
        UUID userId = AuthenticatedUser.userId(authentication);
        int updated = notificationService.markAllRead(userId);
        return ResponseEntity.ok(Map.of("marked", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable("id") UUID id,
            Authentication authentication) {
        UUID userId = AuthenticatedUser.userId(authentication);
        boolean deleted = notificationService.delete(userId, id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/preferences")
    public ResponseEntity<List<NotificationPreferenceDto>> getPreferences(Authentication authentication) {
        UUID userId = AuthenticatedUser.userId(authentication);
        return ResponseEntity.ok(notificationService.getPreferences(userId));
    }

    public record UpdatePreferenceRequest(boolean enabled) {}

    @PutMapping("/preferences/{type}")
    public ResponseEntity<NotificationPreferenceDto> updatePreference(
            @PathVariable("type") NotificationType type,
            @RequestBody UpdatePreferenceRequest request,
            Authentication authentication) {
        UUID userId = AuthenticatedUser.userId(authentication);
        return ResponseEntity.ok(notificationService.updatePreference(
                userId, type, request.enabled()));
    }

    @GetMapping("/workflow-settings/{workflowId}")
    public ResponseEntity<Map<String, Object>> getWorkflowSetting(
            @PathVariable("workflowId") UUID workflowId,
            Authentication authentication) {
        UUID userId = AuthenticatedUser.userId(authentication);
        WorkflowNotifyMode mode = notificationService.getWorkflowSetting(userId, workflowId);
        return ResponseEntity.ok(Map.of(
                "workflowId", workflowId,
                "notifyMode", mode
        ));
    }

    public record UpdateWorkflowSettingRequest(@NotNull WorkflowNotifyMode notifyMode) {}

    @PutMapping("/workflow-settings/{workflowId}")
    public ResponseEntity<WorkflowNotificationSetting> updateWorkflowSetting(
            @PathVariable("workflowId") UUID workflowId,
            @Valid @RequestBody UpdateWorkflowSettingRequest request,
            Authentication authentication) {
        UUID userId = AuthenticatedUser.userId(authentication);
        return ResponseEntity.ok(notificationService.updateWorkflowSetting(userId, workflowId, request.notifyMode()));
    }
}
