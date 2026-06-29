package com.crescendo.workflow.workflow_query;

import com.crescendo.BaseIntegrationTest;
import com.crescendo.enums.WorkflowStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowQueryRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private Workflow_queryRepository queryRepository;

    private Workflow_query savedQueryWorkflow;

    @BeforeEach
    void setUp() {
        queryRepository.deleteAll();

        savedQueryWorkflow = new Workflow_query(
                UUID.randomUUID(),
                "Repo Test Workflow",
                "Repo Description",
                UUID.randomUUID(),
                null,
                false,
                WorkflowStatus.NEVER_RUN,
                0
        );
        queryRepository.save(savedQueryWorkflow);
    }

    @Test
    void updateStepCount_updatesSuccessfully() {
        queryRepository.updateStepCount(savedQueryWorkflow.getId(), 5);

        Optional<Workflow_query> result = queryRepository.findById(savedQueryWorkflow.getId());
        assertTrue(result.isPresent());
        assertEquals(5, result.get().getStep_count());
        assertNotNull(result.get().getUpdatedAt(), "UpdatedAt should be set by the query");
    }

    @Test
    void updateIsActive_updatesSuccessfully() {
        queryRepository.updateIsActive(savedQueryWorkflow.getId(), true);

        Optional<Workflow_query> result = queryRepository.findById(savedQueryWorkflow.getId());
        assertTrue(result.isPresent());
        assertTrue(result.get().isActive());
        assertNotNull(result.get().getUpdatedAt(), "UpdatedAt should be set by the query");
    }
}
