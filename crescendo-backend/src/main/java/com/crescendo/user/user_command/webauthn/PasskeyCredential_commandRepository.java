package com.crescendo.user.user_command.webauthn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasskeyCredential_commandRepository extends JpaRepository<PasskeyCredential_command, UUID> {
    Optional<PasskeyCredential_command> findByCredentialId(byte[] credentialId);
    List<PasskeyCredential_command> findByUserId(UUID userId);
    long countByUserId(UUID userId);
}
