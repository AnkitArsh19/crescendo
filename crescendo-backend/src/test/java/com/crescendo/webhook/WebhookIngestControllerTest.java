package com.crescendo.webhook;

import com.crescendo.execution.condition.ConditionEvaluator;
import com.crescendo.logbook.LogbookDto;
import com.crescendo.logbook.workflow_run.WorkflowRunRepository;
import com.crescendo.logbook.workflow_run.WorkflowRunService;
import com.crescendo.steps.step_condition.StepConditionRepository;
import com.crescendo.steps.steps_command.Steps_command;
import com.crescendo.steps.steps_command.Steps_commandRepository;
import com.crescendo.workflow.workflow_command.Workflow_command;
import com.crescendo.user.user_command.User_command;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookIngestControllerTest {

    @Mock
    private WebhookRepository webhookRepo;
    
    @Mock
    private Steps_commandRepository stepsRepo;
    
    @Mock
    private StepConditionRepository conditionRepo;
    
    @Mock
    private ConditionEvaluator conditionEvaluator;
    
    @Mock
    private WorkflowRunService workflowRunService;
    
    @Mock
    private WorkflowRunRepository workflowRunRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WebhookIngestController controller;

    private MockHttpServletRequest request;
    private Webhook activeWebhook;
    private Steps_command triggerStep;
    private Workflow_command workflow;
    private User_command user;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        
        user = new User_command();
        user.setId(UUID.randomUUID());

        workflow = new Workflow_command();
        workflow.setId(UUID.randomUUID());
        workflow.setUser(user);
        workflow.setActive(true);

        triggerStep = new Steps_command();
        triggerStep.setId(UUID.randomUUID());
        triggerStep.setWorkflow(workflow);

        activeWebhook = new Webhook(
            UUID.randomUUID(), 
            "test-key-12345678901234",
            triggerStep.getId(), 
            true
        );
        ReflectionTestUtils.setField(activeWebhook, "secretKey", "secret");
        ReflectionTestUtils.setField(activeWebhook, "providerSignatureHeader", "X-Hub-Signature-256");
    }

    @Test
    void ingest_whenWebhookNotFound_returns404() {
        when(webhookRepo.findActiveByWebhookKey("invalid-key-12345678901")).thenReturn(Optional.empty());

        ResponseEntity<Object> response = controller.ingest("invalid-key-12345678901", null, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void ingest_whenInvalidSignature_returns401() {
        when(webhookRepo.findActiveByWebhookKey("test-key-12345678901234")).thenReturn(Optional.of(activeWebhook));
        // Missing header in request, verifySignature will return false

        ResponseEntity<Object> response = controller.ingest("test-key-12345678901234", "{}", request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void ingest_whenValidSignatureAndNoConditions_startsRun() throws Exception {
        // Setup valid signature
        String payload = "{\"event\":\"push\"}";

        
        // Wait, verifySignature uses Mac.getInstance("HmacSHA256") with "secret"
        String secret = "secret";
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String validHex = java.util.HexFormat.of().formatHex(hash);
        
        request.addHeader("X-Hub-Signature-256", "sha256=" + validHex);
        request.setMethod("POST");

        when(webhookRepo.findActiveByWebhookKey("test-key-12345678901234")).thenReturn(Optional.of(activeWebhook));
        when(stepsRepo.findByIdAndDeletedAtIsNull(activeWebhook.getStepId())).thenReturn(Optional.of(triggerStep));
        
        Map<String, Object> parsedPayload = Map.of("event", "push");
        when(objectMapper.readValue(payload, Map.class)).thenReturn(parsedPayload);
        when(conditionRepo.findByStepId(triggerStep.getId())).thenReturn(List.of());
        when(conditionEvaluator.evaluate(any(), any())).thenReturn(true);
        
        LogbookDto.WorkflowRunSummaryResponse runResponse = new LogbookDto.WorkflowRunSummaryResponse(
                UUID.randomUUID().toString(),
                workflow.getId().toString(),
                "PENDING",
                null,
                0, 0, 0,
                Instant.now(),
                null
        );
        when(workflowRunService.startRun(eq(user.getId()), eq(workflow.getId()), any()))
            .thenReturn(runResponse);

        ResponseEntity<Object> response = controller.ingest("test-key-12345678901234", payload, request);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(workflowRunService, times(1)).startRun(eq(user.getId()), eq(workflow.getId()), any());
    }
}
