package com.crescendo.steps.steps_query;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface Steps_queryRepository extends JpaRepository<Steps_query, UUID> {
}
