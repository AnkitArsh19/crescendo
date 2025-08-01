package com.crescendo.emailservice.apikey.key_command;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ApiKey_commandRepository extends JpaRepository<ApiKey_command, UUID> {
}
