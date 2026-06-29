package com.crescendo.workflow;

import com.crescendo.BaseIntegrationTest;
import com.crescendo.security.access.AccessControlService;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import com.crescendo.workflow.workflow_command.Workflow_command;
import com.crescendo.workflow.workflow_command.Workflow_commandRepository;
import com.crescendo.workflow.workflow_command.Workflow_commandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

class WorkflowLockingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private Workflow_commandService commandService;

    @Autowired
    private Workflow_commandRepository commandRepo;

    @Autowired
    private User_commandRepository userRepo;

    @MockitoBean
    private AccessControlService accessControlService;

    private User_command testUser;
    private Workflow_command workflow;

    @BeforeEach
    void setUp() {
        commandRepo.deleteAll();
        userRepo.deleteAll();

        testUser = new User_command();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail(com.crescendo.shared.domain.valueobject.Email.of("locking-test@test.com"));
        testUser.setUserName("lockingtest");
        testUser.setRole(com.crescendo.enums.UserRole.USER);
        userRepo.save(testUser);

        doNothing().when(accessControlService).enforceWorkflowLimit(any());
        
        workflow = new Workflow_command(UUID.randomUUID(), "Locking Test", "Desc", testUser, false);
        workflow.setUpdatedAt(Instant.parse("2026-01-01T10:00:00Z"));
        commandRepo.save(workflow);
    }

    @Test
    void saveGraph_throwsConflict_onStaleRevision() {
        WorkflowDto.WorkflowGraphRequest staleRequest = new WorkflowDto.WorkflowGraphRequest(
                "Updated Name",
                "Updated Desc",
                99L, // Different from actual version
                Collections.emptyList(),
                Collections.emptyList()
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> 
            commandService.saveGraph(testUser.getId(), workflow.getId(), staleRequest)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Workflow has been modified"));
    }

    @Test
    void saveGraph_succeeds_onMatchingRevision() {
        WorkflowDto.WorkflowGraphRequest validRequest = new WorkflowDto.WorkflowGraphRequest(
                "Updated Name",
                "Updated Desc",
                0L, // Matches actual version
                Collections.emptyList(),
                Collections.emptyList()
        );

        WorkflowDto.WorkflowGraphResponse response = commandService.saveGraph(testUser.getId(), workflow.getId(), validRequest);
        
        assertNotNull(response);
        assertNotEquals(0L, response.revision(), "Revision should be updated");
    }
}
