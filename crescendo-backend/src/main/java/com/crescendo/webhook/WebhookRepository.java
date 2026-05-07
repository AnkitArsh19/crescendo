package com.crescendo.webhook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookRepository extends JpaRepository<Webhook, UUID> {

    @Query("SELECT w FROM Webhook w WHERE w.webhookKey.value = :key AND w.isActive = true")
    Optional<Webhook> findActiveByWebhookKey(String key);

    Optional<Webhook> findByStepId(UUID stepId);

    List<Webhook> findByStepIdIn(List<UUID> stepIds);
}
