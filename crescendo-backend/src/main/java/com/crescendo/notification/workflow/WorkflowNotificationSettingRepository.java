package com.crescendo.notification.workflow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowNotificationSettingRepository extends JpaRepository<WorkflowNotificationSetting, UUID> {

    Optional<WorkflowNotificationSetting> findByUserIdAndWorkflowId(UUID userId, UUID workflowId);
}
