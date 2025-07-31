package com.crescendo.workflow.workflow_query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface Workflow_queryRepository extends JpaRepository<Workflow_query, UUID> {
}
