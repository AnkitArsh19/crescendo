package com.crescendo.steps.steps_command;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface Steps_commandRepository extends JpaRepository<Steps_command, UUID>{
}
