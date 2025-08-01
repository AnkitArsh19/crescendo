package com.crescendo.webhook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WebhookRepository extends JpaRepository<Webhook, UUID> {
}
