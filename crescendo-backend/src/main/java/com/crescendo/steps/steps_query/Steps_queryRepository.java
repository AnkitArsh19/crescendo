package com.crescendo.steps.steps_query;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface Steps_queryRepository extends JpaRepository<Steps_query, UUID> {

    /// Steps for a workflow, ordered by step_order ascending.
    List<Steps_query> findAllByWorkflowIdOrderByOrderAsc(UUID workflowId);

    /// Remove all query-side steps when a workflow is deleted from the query DB.
    void deleteAllByWorkflowId(UUID workflowId);
}
