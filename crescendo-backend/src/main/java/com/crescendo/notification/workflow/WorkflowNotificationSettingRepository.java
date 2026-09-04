package com.crescendo.notification.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowNotificationSettingRepository extends JpaRepository<WorkflowNotificationSetting, UUID> {

    Optional<WorkflowNotificationSetting> findByUserIdAndWorkflowId(UUID userId, UUID workflowId);
}
