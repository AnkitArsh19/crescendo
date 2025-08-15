package com.crescendo.user.user_command.user_credential;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserCredentialRepository extends JpaRepository<UserCredential, UUID> {
	java.util.Optional<UserCredential> findByUser_Id(UUID userId);
}
