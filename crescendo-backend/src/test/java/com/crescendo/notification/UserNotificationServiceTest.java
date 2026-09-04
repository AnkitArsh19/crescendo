package com.crescendo.notification;

import com.crescendo.notification.preference.NotificationPreference;
import com.crescendo.notification.preference.NotificationPreferenceDto;
import com.crescendo.notification.preference.NotificationPreferenceRepository;
import com.crescendo.notification.workflow.WorkflowNotificationSetting;
import com.crescendo.notification.workflow.WorkflowNotificationSetting.WorkflowNotifyMode;
import com.crescendo.notification.workflow.WorkflowNotificationSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserNotificationServiceTest {

    @Mock
    private UserNotificationRepository userNotificationRepository;

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Mock
    private WorkflowNotificationSettingRepository workflowNotificationSettingRepository;

    @Mock
    private NotificationSseService notificationSseService;

    private UserNotificationService userNotificationService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userNotificationService = new UserNotificationService(
                userNotificationRepository,
                notificationPreferenceRepository,
                workflowNotificationSettingRepository,
                notificationSseService
        );
        userId = UUID.randomUUID();
    }

    @Test
    void testCreateNotification_DefaultEnabled() {
        when(notificationPreferenceRepository.findByUserIdAndType(userId, NotificationType.LOGIN_NEW_DEVICE))
                .thenReturn(Optional.empty());
        when(userNotificationRepository.save(any(UserNotification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UserNotification result = userNotificationService.create(
                userId,
                NotificationType.LOGIN_NEW_DEVICE,
                "New Sign-in",
                "Chrome on Windows",
                Map.of("ip", "1.2.3.4")
        );

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("New Sign-in", result.getTitle());
        verify(userNotificationRepository, times(1)).save(any(UserNotification.class));
        verify(notificationSseService, times(1)).broadcastNotification(eq(userId), any(UserNotificationDto.class));
    }

    @Test
    void testCreateNotification_Disabled() {
        NotificationPreference pref = new NotificationPreference(userId, NotificationType.LOGIN_NEW_DEVICE);
        pref.setEnabled(false);

        when(notificationPreferenceRepository.findByUserIdAndType(userId, NotificationType.LOGIN_NEW_DEVICE))
                .thenReturn(Optional.of(pref));

        UserNotification result = userNotificationService.create(
                userId,
                NotificationType.LOGIN_NEW_DEVICE,
                "New Sign-in",
                "Chrome on Windows",
                Map.of()
        );

        assertNull(result); // Notification was not saved
        verify(userNotificationRepository, never()).save(any());
        verify(notificationSseService, never()).broadcastNotification(any(), any());
    }

    @Test
    void testCreateWorkflowNotification_FailureOnlyMode_SkipsSuccess() {
        UUID workflowId = UUID.randomUUID();
        when(notificationPreferenceRepository.findByUserIdAndType(userId, NotificationType.WORKFLOW_RUN_SUCCESS))
                .thenReturn(Optional.empty());

        WorkflowNotificationSetting setting = new WorkflowNotificationSetting(userId, workflowId, WorkflowNotifyMode.FAILURE_ONLY);
        when(workflowNotificationSettingRepository.findByUserIdAndWorkflowId(userId, workflowId))
                .thenReturn(Optional.of(setting));

        UserNotification result = userNotificationService.create(
                userId,
                NotificationType.WORKFLOW_RUN_SUCCESS,
                "Success",
                "Run completed",
                Map.of("workflowId", workflowId.toString())
        );

        assertNull(result);
        verify(userNotificationRepository, never()).save(any());
    }

    @Test
    void testCreateWorkflowNotification_FailureOnlyMode_AllowsFailure() {
        UUID workflowId = UUID.randomUUID();
        when(notificationPreferenceRepository.findByUserIdAndType(userId, NotificationType.WORKFLOW_RUN_FAILED))
                .thenReturn(Optional.empty());

        WorkflowNotificationSetting setting = new WorkflowNotificationSetting(userId, workflowId, WorkflowNotifyMode.FAILURE_ONLY);
        when(workflowNotificationSettingRepository.findByUserIdAndWorkflowId(userId, workflowId))
                .thenReturn(Optional.of(setting));
        when(userNotificationRepository.save(any(UserNotification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UserNotification result = userNotificationService.create(
                userId,
                NotificationType.WORKFLOW_RUN_FAILED,
                "Failed",
                "Run failed",
                Map.of("workflowId", workflowId.toString())
        );

        assertNotNull(result);
        verify(userNotificationRepository, times(1)).save(any());
    }

    @Test
    void testCreateWorkflowNotification_NeverMode() {
        UUID workflowId = UUID.randomUUID();
        when(notificationPreferenceRepository.findByUserIdAndType(userId, NotificationType.WORKFLOW_RUN_FAILED))
                .thenReturn(Optional.empty());

        WorkflowNotificationSetting setting = new WorkflowNotificationSetting(userId, workflowId, WorkflowNotifyMode.NEVER);
        when(workflowNotificationSettingRepository.findByUserIdAndWorkflowId(userId, workflowId))
                .thenReturn(Optional.of(setting));

        UserNotification result = userNotificationService.create(
                userId,
                NotificationType.WORKFLOW_RUN_FAILED,
                "Failed",
                "Run failed",
                Map.of("workflowId", workflowId.toString())
        );

        assertNull(result);
        verify(userNotificationRepository, never()).save(any());
    }

    @Test
    void testGetPreferences_ReturnsAllTypesWithDefaults() {
        when(notificationPreferenceRepository.findByUserId(userId)).thenReturn(List.of());

        List<NotificationPreferenceDto> prefs = userNotificationService.getPreferences(userId);

        assertEquals(NotificationType.values().length, prefs.size());
        assertTrue(prefs.stream().allMatch(NotificationPreferenceDto::enabled));
    }

    @Test
    void testMarkReadAndUnreadCount() {
        when(userNotificationRepository.countByUserIdAndIsReadFalse(userId)).thenReturn(5L);
        assertEquals(5L, userNotificationService.getUnreadCount(userId));

        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(userNotificationRepository.markReadByIds(userId, ids)).thenReturn(2);
        assertEquals(2, userNotificationService.markRead(userId, ids));

        when(userNotificationRepository.markAllReadByUserId(userId)).thenReturn(5);
        assertEquals(5, userNotificationService.markAllRead(userId));
    }
}
