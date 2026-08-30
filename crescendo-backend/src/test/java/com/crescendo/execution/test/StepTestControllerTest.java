package com.crescendo.execution.test;

import com.crescendo.connections.connections_command.Connections_commandRepository;
import com.crescendo.connections.oauth.OAuthTokenRefreshService;
import com.crescendo.execution.action.ActionHandlerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StepTestControllerTest {

    private final StepSetupValidationService setupValidationService = mock(StepSetupValidationService.class);
    private final ActionHandlerRegistry handlerRegistry = mock(ActionHandlerRegistry.class);
    private final OAuthTokenRefreshService tokenService = mock(OAuthTokenRefreshService.class);
    private final Connections_commandRepository connectionRepository = mock(Connections_commandRepository.class);
    private final TriggerSampleService triggerSampleService = mock(TriggerSampleService.class);

    private final StepTestController controller = new StepTestController(
            setupValidationService, handlerRegistry, tokenService, connectionRepository, triggerSampleService);

    private final UUID testUserId = UUID.randomUUID();
    private final com.crescendo.user.user_command.User_command user = new com.crescendo.user.user_command.User_command(
            testUserId, "testuser", "test@example.com", com.crescendo.enums.UserRole.USER);
    private final com.crescendo.security.AppUserDetails principal = com.crescendo.security.AppUserDetails.from(user, java.util.Optional.empty());
    private final Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    private final UUID userId = principal.getId();

    @Test
    void validateStepReturnsSetupValidationResultWithoutExecutingHandlers() {
        StepSetupValidationService.SetupValidationResult setupResult = new StepSetupValidationService.SetupValidationResult(
                true,
                List.of(new StepSetupValidationService.SetupCheck("conn", "Connection", "PASS", "Connected")),
                Map.of("dataIn", Map.of("email", "test@example.com")),
                null,
                Map.of("setupPolicy", "READ_TARGET")
        );

        when(setupValidationService.validate(eq("gmail"), eq("send-email"), eq(false), isNull(), anyMap(), anyMap(), eq(userId)))
                .thenReturn(setupResult);

        StepTestController.TestStepRequest request = new StepTestController.TestStepRequest(
                "gmail", "send-email", null, false, null, Map.of("email", "test@example.com"), Map.of(), false);

        ResponseEntity<StepTestController.TestStepResponse> response = controller.validate(request, auth);

        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals("SETUP_CHECK", response.getBody().mode());
        assertEquals(1, response.getBody().checks().size());
    }

    @Test
    void triggerSampleEndpointReturnsSampleRecord() {
        StepSetupValidationService.SetupValidationResult setupResult = new StepSetupValidationService.SetupValidationResult(
                true,
                List.of(new StepSetupValidationService.SetupCheck("conn", "Connection", "PASS", "Trigger verified")),
                Map.of(),
                null,
                Map.of("setupPolicy", "READ_SAMPLE")
        );

        when(setupValidationService.validate(eq("schedule"), eq("cron"), eq(true), isNull(), anyMap(), anyMap(), eq(userId)))
                .thenReturn(setupResult);

        when(triggerSampleService.getTriggerSample(eq("schedule"), eq("cron"), isNull(), anyMap(), eq(userId)))
                .thenReturn(Map.of("scheduledTime", "2026-08-30T12:00:00Z", "interval", "1h"));

        StepTestController.TestStepRequest request = new StepTestController.TestStepRequest(
                "schedule", null, "cron", true, null, Map.of("interval", "1h"), Map.of(), false);

        ResponseEntity<StepTestController.TestStepResponse> response = controller.triggerSample(request, auth);

        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals("TRIGGER_SAMPLE", response.getBody().mode());
        assertEquals("2026-08-30T12:00:00Z", response.getBody().data().get("scheduledTime"));
    }

    @Test
    void liveRunRequiresExplicitAcknowledgement() {
        StepTestController.TestStepRequest request = new StepTestController.TestStepRequest(
                "slack", "send-message", null, false, null, Map.of(), Map.of(), false);

        ResponseEntity<StepTestController.TestStepResponse> response = controller.runLive(request, auth);

        assertEquals(409, response.getStatusCode().value());
        assertFalse(response.getBody().success());
        assertTrue(response.getBody().error().contains("Confirm that this live run may change data"));
    }
}
