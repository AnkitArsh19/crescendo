package com.crescendo.execution.engine;

import com.crescendo.execution.action.ActionHandlerRegistry;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionResult;
import com.crescendo.logbook.step_run.StepRunService;
import com.crescendo.logbook.LogbookDto;
import com.crescendo.logbook.workflow_run.WorkflowRunService;
import com.crescendo.steps.steps_command.Steps_command;
import com.crescendo.steps.steps_command.Steps_commandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowExecutionEngineTest {

    @Mock
    private Steps_commandRepository stepsRepo;

    @Mock
    private ActionHandlerRegistry handlerRegistry;

    @Mock
    private WorkflowRunService workflowRunService;

    @Mock
    private com.crescendo.user.user_query.User_queryRepository userQueryRepo;

    @Mock
    private StepRunService stepRunService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WorkflowExecutionEngine engine;

    @BeforeEach
    void setUp() {
        // Initialization if needed
    }

    @Test
    void execute_whenNoExecutableSteps_completesImmediately() {
        com.crescendo.logbook.workflow_run.WorkflowRun run = mock(com.crescendo.logbook.workflow_run.WorkflowRun.class);
        UUID runId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();

        when(run.getId()).thenReturn(runId);
        when(run.getUserId()).thenReturn(userId);
        when(run.getWorkflowId()).thenReturn(workflowId);
        when(run.getTriggerData()).thenReturn(Map.of("hello", "world"));

        when(stepsRepo.findActiveByWorkflowIdOrdered(workflowId)).thenReturn(List.of());

        engine.execute(run);

        verify(workflowRunService, times(1)).completeRun(userId, runId);
    }

    @Test
    void execute_withExecutableSteps_completesSuccessfully() {
        com.crescendo.logbook.workflow_run.WorkflowRun run = mock(com.crescendo.logbook.workflow_run.WorkflowRun.class);
        UUID runId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID workflowId = UUID.randomUUID();

        when(run.getId()).thenReturn(runId);
        when(run.getUserId()).thenReturn(userId);
        when(run.getWorkflowId()).thenReturn(workflowId);
        when(run.getTriggerData()).thenReturn(Map.of("trigger", "data"));

        Steps_command step1 = new Steps_command();
        step1.setId(UUID.randomUUID());
        step1.setName("step1");
        step1.setAppKey("test");
        step1.setActionKey("action");
        step1.setOrder(new com.crescendo.shared.domain.valueobject.StepOrder(java.math.BigDecimal.ONE));

        when(stepsRepo.findActiveByWorkflowIdOrdered(workflowId)).thenReturn(List.of(step1));

        ActionHandler mockHandler = mock(ActionHandler.class);
        when(handlerRegistry.find("test", "action")).thenReturn(java.util.Optional.of(mockHandler));

        LogbookDto.StepRunResponse stepRunResponse = new LogbookDto.StepRunResponse(
                UUID.randomUUID().toString(),
                step1.getId().toString(),
                "RUNNING",
                Map.of(),
                null,
                null,
                null,
                null);
        when(stepRunService.startStepRun(eq(userId), eq(runId), eq(step1.getId()), any()))
                .thenReturn(stepRunResponse);

        when(mockHandler.execute(any())).thenReturn(ActionResult.success(Map.of("out", "val")));

        engine.execute(run);

        verify(stepRunService, times(1)).completeStepRun(eq(userId), any(UUID.class), org.mockito.ArgumentMatchers.<Map<String, Object>>any());
        verify(workflowRunService, times(1)).completeRun(userId, runId);
    }
}
