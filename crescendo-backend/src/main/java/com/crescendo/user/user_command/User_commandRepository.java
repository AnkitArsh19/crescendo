package com.crescendo.user.user_command;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface User_commandRepository extends JpaRepository<User_command, UUID> {
}
