package com.crescendo.notification;

import com.crescendo.notification.preference.NotificationPreference;
import com.crescendo.notification.preference.NotificationPreferenceDto;
import com.crescendo.notification.preference.NotificationPreferenceRepository;
import com.crescendo.notification.workflow.WorkflowNotificationSetting;
import com.crescendo.notification.workflow.WorkflowNotificationSetting.WorkflowNotifyMode;
import com.crescendo.notification.workflow.WorkflowNotificationSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class UserNotificationService {

    private static final Logger log = LoggerFactory.getLogger(UserNotificationService.class);

    private final UserNotificationRepository userNotificationRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final WorkflowNotificationSettingRepository workflowNotificationSettingRepository;
    private final NotificationSseService notificationSseService;

    public UserNotificationService(
            UserNotificationRepository userNotificationRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            WorkflowNotificationSettingRepository workflowNotificationSettingRepository,
            NotificationSseService notificationSseService) {
        this.userNotificationRepository = userNotificationRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.workflowNotificationSettingRepository = workflowNotificationSettingRepository;
        this.notificationSseService = notificationSseService;
    }

    /**
     * Central creation method. Checks preferences and per-workflow settings,
     * persists if enabled, and pushes in real-time via SSE & Redis Pub/Sub.
     */
    @Transactional
    public UserNotification create(UUID userId, NotificationType type, String title, String body, Map<String, Object> metadata) {
        if (userId == null || type == null) {
            return null;
        }

        // 1. Check user preference for this type
        Optional<NotificationPreference> prefOpt = notificationPreferenceRepository.findByUserIdAndType(userId, type);
        boolean enabled = prefOpt.map(NotificationPreference::isEnabled).orElse(true);
        if (!enabled) {
            return null;
        }

        // 2. Check workflow-specific setting if applicable
        if (isWorkflowRunType(type) && metadata != null && metadata.containsKey("workflowId")) {
            try {
                UUID workflowId = UUID.fromString(metadata.get("workflowId").toString());
                Optional<WorkflowNotificationSetting> setting = workflowNotificationSettingRepository.findByUserIdAndWorkflowId(userId, workflowId);
                WorkflowNotifyMode mode = setting.map(WorkflowNotificationSetting::getNotifyMode).orElse(WorkflowNotifyMode.ALWAYS);

                if (mode == WorkflowNotifyMode.NEVER) {
                    return null;
                }
                if (mode == WorkflowNotifyMode.FAILURE_ONLY && type == NotificationType.WORKFLOW_RUN_SUCCESS) {
                    return null;
                }
            } catch (Exception e) {
                log.debug("Could not parse workflowId from notification metadata: {}", e.getMessage());
            }
        }

        // 3. Persist and broadcast via SSE
        UserNotification notification = new UserNotification(
                UUID.randomUUID(),
                userId,
                type,
                title,
                body,
                metadata != null ? metadata : Map.of()
        );
        UserNotification saved = userNotificationRepository.save(notification);

        UserNotificationDto dto = UserNotificationDto.from(saved);
        notificationSseService.broadcastNotification(userId, dto);

        return saved;
    }

    private boolean isWorkflowRunType(NotificationType type) {
        return type == NotificationType.WORKFLOW_RUN_SUCCESS
                || type == NotificationType.WORKFLOW_RUN_FAILED
                || type == NotificationType.WORKFLOW_RUN_CANCELLED;
    }

    @Transactional(readOnly = true)
    public Page<UserNotificationDto> getNotifications(UUID userId, String filter, Pageable pageable) {
        if ("unread".equalsIgnoreCase(filter)) {
            return userNotificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable)
                    .map(UserNotificationDto::from);
        }
        return userNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(UserNotificationDto::from);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return userNotificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public int markRead(UUID userId, List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return userNotificationRepository.markReadByIds(userId, ids);
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return userNotificationRepository.markAllReadByUserId(userId);
    }

    @Transactional
    public boolean delete(UUID userId, UUID id) {
        return userNotificationRepository.findById(id)
                .filter(n -> n.getUserId().equals(userId))
                .map(n -> {
                    userNotificationRepository.delete(n);
                    return true;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<NotificationPreferenceDto> getPreferences(UUID userId) {
        Map<NotificationType, NotificationPreference> map = new EnumMap<>(NotificationType.class);
        for (NotificationPreference p : notificationPreferenceRepository.findByUserId(userId)) {
            map.put(p.getType(), p);
        }

        List<NotificationPreferenceDto> dtos = new ArrayList<>();
        for (NotificationType type : NotificationType.values()) {
            NotificationPreference p = map.get(type);
            boolean enabled = p == null || p.isEnabled();
            dtos.add(NotificationPreferenceDto.of(type, enabled));
        }
        return dtos;
    }

    @Transactional
    public NotificationPreferenceDto updatePreference(UUID userId, NotificationType type, boolean enabled) {
        NotificationPreference pref = notificationPreferenceRepository.findByUserIdAndType(userId, type)
                .orElseGet(() -> new NotificationPreference(userId, type));
        pref.setEnabled(enabled);
        notificationPreferenceRepository.save(pref);
        return NotificationPreferenceDto.of(type, enabled);
    }

    @Transactional(readOnly = true)
    public WorkflowNotifyMode getWorkflowSetting(UUID userId, UUID workflowId) {
        return workflowNotificationSettingRepository.findByUserIdAndWorkflowId(userId, workflowId)
                .map(WorkflowNotificationSetting::getNotifyMode)
                .orElse(WorkflowNotifyMode.ALWAYS);
    }

    @Transactional
    public WorkflowNotificationSetting updateWorkflowSetting(UUID userId, UUID workflowId, WorkflowNotifyMode mode) {
        WorkflowNotificationSetting setting = workflowNotificationSettingRepository.findByUserIdAndWorkflowId(userId, workflowId)
                .orElseGet(() -> new WorkflowNotificationSetting(userId, workflowId, mode));
        setting.setNotifyMode(mode);
        return workflowNotificationSettingRepository.save(setting);
    }
}
