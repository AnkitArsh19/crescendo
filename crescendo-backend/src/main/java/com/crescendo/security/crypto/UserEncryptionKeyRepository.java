package com.crescendo.security.crypto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserEncryptionKeyRepository extends JpaRepository<UserEncryptionKey, UUID> {
}
