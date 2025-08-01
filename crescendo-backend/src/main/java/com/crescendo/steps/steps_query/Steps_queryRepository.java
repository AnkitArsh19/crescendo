package com.crescendo.steps.steps_query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface Steps_queryRepository extends JpaRepository<Steps_query, UUID> {
}
