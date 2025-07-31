package com.crescendo.workflow.workflow_command;

import com.crescendo.user.user_command.User_command;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface Workflow_commandRepository extends JpaRepository<User_command, UUID> {
}
