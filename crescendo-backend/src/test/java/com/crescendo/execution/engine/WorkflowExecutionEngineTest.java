package com.crescendo.execution.engine;

import com.crescendo.execution.action.ActionHandlerRegistry;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionResult;
import com.crescendo.execution.expression.WorkflowExpressionResolver;
import com.crescendo.enums.StepType;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowExecutionEngineTest {

    @Mock
    private Steps_commandRepository stepsRepo;

    @Mock
    private com.crescendo.workflow.workflow_command.WorkflowEdge_commandRepository edgeRepo;

    @Mock
    private com.crescendo.connections.connections_command.Connections_commandRepository connectionsRepo;

    @Mock
    private com.crescendo.connections.oauth.OAuthTokenRefreshService tokenRefreshService;

    @Mock
    private com.crescendo.connections.security.ConnectionCredentialsCryptoService credentialsCryptoService;

    @Mock
    private com.crescendo.admin.PlatformKeyRepository platformKeyRepo;

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

    @Mock
    private WorkflowExpressionResolver expressionResolver;

    @InjectMocks
    private WorkflowExecutionEngine engine;

    @BeforeEach
    void setUp() {
        var connection = mock(com.crescendo.connections.connections_command.Connections_command.class);
        when(edgeRepo.findByWorkflowId(any())).thenReturn(List.of());
        when(connectionsRepo.findByIdAndUser_Id(any(), any())).thenReturn(Optional.of(connection));
        when(tokenRefreshService.getValidCredentials(connection)).thenReturn(Map.of("accessToken", "test-token"));
        when(expressionResolver.resolveConfiguration(anyMap(), anyMap(), anyMap()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(stepRunService.startStepRun(any(), any(), any(), any()))
                .thenAnswer(invocation -> new LogbookDto.StepRunResponse(
                        UUID.randomUUID().toString(), "test-step", "RUNNING",
                        Map.of(), null, null, null, null));
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
        step1.setConnectionId(UUID.randomUUID());
        step1.setOrder(new com.crescendo.shared.domain.valueobject.StepOrder(java.math.BigDecimal.ONE));

        Steps_command trigger = step("trigger", StepType.TRIGGER, "webhook", "receive", 0, null);

        when(stepsRepo.findActiveByWorkflowIdOrdered(workflowId)).thenReturn(List.of(trigger, step1));
        when(edgeRepo.findByWorkflowId(workflowId)).thenReturn(List.of(edge(workflowId, trigger, step1, "out")));

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

    @Test
    void execute_branchSelectsOnlyTheMatchingDownstreamPath() {
        UUID workflowId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID connectionId = UUID.randomUUID();

        var run = mock(com.crescendo.logbook.workflow_run.WorkflowRun.class);
        when(run.getId()).thenReturn(runId);
        when(run.getUserId()).thenReturn(userId);
        when(run.getWorkflowId()).thenReturn(workflowId);
        when(run.getTriggerData()).thenReturn(Map.of("event", "created"));

        Steps_command trigger = step("trigger", StepType.TRIGGER, "webhook", "receive", 0, null);
        Steps_command branch = step("if", StepType.ACTION, "logic", "logic:if", 1, connectionId);
        Steps_command onTrue = step("true", StepType.ACTION, "test", "true-action", 2, connectionId);
        Steps_command onFalse = step("false", StepType.ACTION, "test", "false-action", 3, connectionId);

        when(stepsRepo.findActiveByWorkflowIdOrdered(workflowId))
                .thenReturn(List.of(trigger, branch, onTrue, onFalse));
        when(edgeRepo.findByWorkflowId(workflowId)).thenReturn(List.of(
                edge(workflowId, trigger, branch, "out"),
                edge(workflowId, branch, onTrue, "true"),
                edge(workflowId, branch, onFalse, "false")));

        ActionHandler branchHandler = mock(ActionHandler.class);
        ActionHandler trueHandler = mock(ActionHandler.class);
        ActionHandler falseHandler = mock(ActionHandler.class);
        when(handlerRegistry.find("logic", "logic:if")).thenReturn(Optional.of(branchHandler));
        when(handlerRegistry.find("test", "true-action")).thenReturn(Optional.of(trueHandler));
        when(handlerRegistry.find("test", "false-action")).thenReturn(Optional.of(falseHandler));
        when(branchHandler.execute(any())).thenReturn(ActionResult.success(Map.of("_branchKey", "true", "approved", true)));
        when(trueHandler.execute(any())).thenReturn(ActionResult.success(Map.of("delivered", true)));

        engine.execute(run);

        verify(branchHandler).execute(any());
        verify(trueHandler).execute(argThat(context -> Boolean.TRUE.equals(context.input("approved"))));
        verify(falseHandler, never()).execute(any());
        verify(workflowRunService).completeRun(userId, runId);
    }

    @Test
    void execute_logicSwitchSelectsConfiguredOutputBranch() {
        UUID workflowId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID connectionId = UUID.randomUUID();

        var run = mock(com.crescendo.logbook.workflow_run.WorkflowRun.class);
        when(run.getId()).thenReturn(runId);
        when(run.getUserId()).thenReturn(userId);
        when(run.getWorkflowId()).thenReturn(workflowId);
        when(run.getTriggerData()).thenReturn(Map.of("category", "VIP"));

        Steps_command trigger = step("trigger", StepType.TRIGGER, "webhook", "receive", 0, null);
        Steps_command switchStep = step("switch", StepType.ACTION, "logic", "logic:switch", 1, connectionId);
        Steps_command out0 = step("vip_flow", StepType.ACTION, "test", "vip-action", 2, connectionId);
        Steps_command out1 = step("regular_flow", StepType.ACTION, "test", "regular-action", 3, connectionId);

        when(stepsRepo.findActiveByWorkflowIdOrdered(workflowId))
                .thenReturn(List.of(trigger, switchStep, out0, out1));
        when(edgeRepo.findByWorkflowId(workflowId)).thenReturn(List.of(
                edge(workflowId, trigger, switchStep, "out"),
                edge(workflowId, switchStep, out0, "output_0"),
                edge(workflowId, switchStep, out1, "output_1")));

        ActionHandler switchHandler = mock(ActionHandler.class);
        ActionHandler vipHandler = mock(ActionHandler.class);
        ActionHandler regularHandler = mock(ActionHandler.class);
        when(handlerRegistry.find("logic", "logic:switch")).thenReturn(Optional.of(switchHandler));
        when(handlerRegistry.find("test", "vip-action")).thenReturn(Optional.of(vipHandler));
        when(handlerRegistry.find("test", "regular-action")).thenReturn(Optional.of(regularHandler));
        when(switchHandler.execute(any())).thenReturn(ActionResult.success(Map.of("_branchKey", "output_0", "tier", "gold")));
        when(vipHandler.execute(any())).thenReturn(ActionResult.success(Map.of("discount", 20)));

        engine.execute(run);

        verify(switchHandler).execute(any());
        verify(vipHandler).execute(any());
        verify(regularHandler, never()).execute(any());
        verify(workflowRunService).completeRun(userId, runId);
    }

    @Test
    void execute_diamondReconvergence_executesDownstreamStepWithTakenBranchOutput() {
        UUID workflowId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID connectionId = UUID.randomUUID();

        var run = mock(com.crescendo.logbook.workflow_run.WorkflowRun.class);
        when(run.getId()).thenReturn(runId);
        when(run.getUserId()).thenReturn(userId);
        when(run.getWorkflowId()).thenReturn(workflowId);
        when(run.getTriggerData()).thenReturn(Map.of("amount", 100));

        Steps_command trigger = step("trigger", StepType.TRIGGER, "webhook", "receive", 0, null);
        Steps_command branch = step("if", StepType.ACTION, "logic", "logic:if", 1, connectionId);
        Steps_command trueStep = step("trueBranch", StepType.ACTION, "test", "true-act", 2, connectionId);
        Steps_command falseStep = step("falseBranch", StepType.ACTION, "test", "false-act", 3, connectionId);
        Steps_command joinStep = step("sharedJoin", StepType.ACTION, "test", "join-act", 4, connectionId);

        when(stepsRepo.findActiveByWorkflowIdOrdered(workflowId))
                .thenReturn(List.of(trigger, branch, trueStep, falseStep, joinStep));
        when(edgeRepo.findByWorkflowId(workflowId)).thenReturn(List.of(
                edge(workflowId, trigger, branch, "out"),
                edge(workflowId, branch, trueStep, "true"),
                edge(workflowId, branch, falseStep, "false"),
                edge(workflowId, trueStep, joinStep, "out"),
                edge(workflowId, falseStep, joinStep, "out")));

        ActionHandler branchHandler = mock(ActionHandler.class);
        ActionHandler trueHandler = mock(ActionHandler.class);
        ActionHandler falseHandler = mock(ActionHandler.class);
        ActionHandler joinHandler = mock(ActionHandler.class);
        when(handlerRegistry.find("logic", "logic:if")).thenReturn(Optional.of(branchHandler));
        when(handlerRegistry.find("test", "true-act")).thenReturn(Optional.of(trueHandler));
        when(handlerRegistry.find("test", "false-act")).thenReturn(Optional.of(falseHandler));
        when(handlerRegistry.find("test", "join-act")).thenReturn(Optional.of(joinHandler));

        when(branchHandler.execute(any())).thenReturn(ActionResult.success(Map.of("_branchKey", "true")));
        when(trueHandler.execute(any())).thenReturn(ActionResult.success(Map.of("branchResult", "trueDone")));
        when(joinHandler.execute(any())).thenReturn(ActionResult.success(Map.of("final", "ok")));

        engine.execute(run);

        verify(trueHandler).execute(any());
        verify(falseHandler, never()).execute(any());
        verify(joinHandler, times(1)).execute(any());
        verify(workflowRunService).completeRun(userId, runId);
    }

    @Test
    void execute_stepFailureInBranch_abortsRunAndDoesNotExecuteDownstream() {
        UUID workflowId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID connectionId = UUID.randomUUID();

        var run = mock(com.crescendo.logbook.workflow_run.WorkflowRun.class);
        when(run.getId()).thenReturn(runId);
        when(run.getUserId()).thenReturn(userId);
        when(run.getWorkflowId()).thenReturn(workflowId);
        when(run.getTriggerData()).thenReturn(Map.of("event", "test"));

        Steps_command trigger = step("trigger", StepType.TRIGGER, "webhook", "receive", 0, null);
        Steps_command failingStep = step("failing", StepType.ACTION, "test", "fail-act", 1, connectionId);
        Steps_command downstreamStep = step("downstream", StepType.ACTION, "test", "downstream-act", 2, connectionId);

        when(stepsRepo.findActiveByWorkflowIdOrdered(workflowId))
                .thenReturn(List.of(trigger, failingStep, downstreamStep));
        when(edgeRepo.findByWorkflowId(workflowId)).thenReturn(List.of(
                edge(workflowId, trigger, failingStep, "out"),
                edge(workflowId, failingStep, downstreamStep, "out")));

        ActionHandler failHandler = mock(ActionHandler.class);
        ActionHandler downstreamHandler = mock(ActionHandler.class);
        when(handlerRegistry.find("test", "fail-act")).thenReturn(Optional.of(failHandler));
        when(handlerRegistry.find("test", "downstream-act")).thenReturn(Optional.of(downstreamHandler));
        when(failHandler.execute(any())).thenReturn(ActionResult.failure("API error: 500"));

        engine.execute(run);

        verify(failHandler).execute(any());
        verify(downstreamHandler, never()).execute(any());
        verify(workflowRunService).failRun(eq(userId), eq(runId), eq("One or more steps failed"));
    }


    private static Steps_command step(String name, com.crescendo.enums.StepType type,
                                      String appKey, String actionKey, int order, UUID connectionId) {
        Steps_command step = new Steps_command();
        step.setId(UUID.randomUUID());
        step.setName(name);
        step.setType(type);
        step.setAppKey(appKey);
        step.setActionKey(actionKey);
        step.setOrder(java.math.BigDecimal.valueOf(order));
        step.setConnectionId(connectionId);
        step.setConfiguration(Map.of());
        return step;
    }

    private static com.crescendo.workflow.workflow_command.WorkflowEdge_command edge(
            UUID workflowId, Steps_command source, Steps_command target, String handle) {
        return new com.crescendo.workflow.workflow_command.WorkflowEdge_command(
                UUID.randomUUID(), workflowId, source.getId(), target.getId(), handle, "in");
    }
}

