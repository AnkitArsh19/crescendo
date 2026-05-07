package com.crescendo.emailservice.emailtemplate.template_query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTemplate_queryRepository extends JpaRepository<EmailTemplate_query, UUID> {

    List<EmailTemplate_query> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<EmailTemplate_query> findByIdAndUserId(UUID id, UUID userId);
}
