package com.crescendo.execution.suspension;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowSuspensionRepository extends JpaRepository<WorkflowSuspension, UUID> {

    Optional<WorkflowSuspension> findByCorrelationKeyAndStatus(String correlationKey, WorkflowSuspension.SuspensionStatus status);

    List<WorkflowSuspension> findByStatusAndTimeoutAtBefore(WorkflowSuspension.SuspensionStatus status, Instant now);
}
