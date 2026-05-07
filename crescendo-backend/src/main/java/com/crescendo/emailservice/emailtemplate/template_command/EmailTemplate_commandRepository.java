package com.crescendo.emailservice.emailtemplate.template_command;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTemplate_commandRepository extends JpaRepository<EmailTemplate_command, UUID> {

    List<EmailTemplate_command> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<EmailTemplate_command> findByIdAndUserId(UUID id, UUID userId);
}
