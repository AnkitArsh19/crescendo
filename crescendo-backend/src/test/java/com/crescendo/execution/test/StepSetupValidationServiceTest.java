package com.crescendo.execution.test;

import com.crescendo.app.App;
import com.crescendo.app.AppRepository;
import com.crescendo.connections.connections_command.Connections_commandRepository;
import com.crescendo.connections.oauth.OAuthTokenRefreshService;
import com.crescendo.enums.AuthType;
import com.crescendo.execution.resource.ResourceFetchService;
import com.crescendo.security.DataSanitizationService;
import com.crescendo.shared.domain.valueobject.AppKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StepSetupValidationServiceTest {

    private final AppRepository appRepository = mock(AppRepository.class);
    private final Connections_commandRepository connectionRepository = mock(Connections_commandRepository.class);
    private final OAuthTokenRefreshService tokenService = mock(OAuthTokenRefreshService.class);
    private final ResourceFetchService resourceFetchService = mock(ResourceFetchService.class);
    private final com.crescendo.execution.expression.WorkflowExpressionResolver expressionResolver = new com.crescendo.execution.expression.WorkflowExpressionResolver();
    private final StepSetupValidationService service = new StepSetupValidationService(
            appRepository, connectionRepository, tokenService, resourceFetchService,
            new DataSanitizationService(), new OperationTestContractFactory(), expressionResolver);

    @Test
    void expressionsAreResolvedAgainstSampleInputDataInPreview() {
        App app = new App("gmail", "Gmail", "", "", AuthType.NONE, List.of(), List.of(Map.of(
                "actionKey", "send-email", "name", "Send Email",
                "configSchema", List.of(
                        Map.of("key", "to", "label", "To", "type", "text", "required", true),
                        Map.of("key", "subject", "label", "Subject", "type", "text", "required", true)
                )
        )));
        when(appRepository.findById(AppKey.of("gmail"))).thenReturn(Optional.of(app));

        Map<String, Object> config = Map.of(
                "to", "{{steps.1.customerEmail}}",
                "subject", "Hello {{name}} on {{today}}"
        );
        Map<String, Object> sampleInput = Map.of(
                "customerEmail", "alice@example.com",
                "name", "Alice"
        );

        StepSetupValidationService.SetupValidationResult result = service.validate(
                "gmail", "send-email", false, null, config, sampleInput, UUID.randomUUID());

        assertTrue(result.success());
        Map<String, Object> dataIn = (Map<String, Object>) result.preview().get("dataIn");
        org.junit.jupiter.api.Assertions.assertEquals("alice@example.com", dataIn.get("to"));
        org.junit.jupiter.api.Assertions.assertTrue(String.valueOf(dataIn.get("subject")).startsWith("Hello Alice on 20"));
    }

    @Test
    void authFreeLogicSetupIsValidatedLocallyWithoutCallingExternalServices() {
        App app = new App("logic", "Logic", "", "", AuthType.NONE, List.of(), List.of(Map.of(
                "actionKey", "logic:if", "name", "If",
                "configSchema", List.of(Map.of("key", "conditions", "label", "Conditions", "type", "json", "required", true))
        )));
        when(appRepository.findById(AppKey.of("logic"))).thenReturn(Optional.of(app));

        StepSetupValidationService.SetupValidationResult result = service.validate(
                "logic", "logic:if", false, null, Map.of("conditions", List.of(Map.of("combinator", "AND"))), Map.of(), UUID.randomUUID());

        assertTrue(result.success());
        assertEqualsContract(result, "LOCAL_SIMULATION", false);
        verifyNoInteractions(connectionRepository, tokenService, resourceFetchService);
    }

    @Test
    void setupCheckDoesNotFallBackToAnyOtherConnectionOrPlatformCredential() {
        App app = new App("slack", "Slack", "", "", AuthType.OAUTH2, List.of(), List.of(Map.of(
                "actionKey", "send-message", "name", "Send message", "configSchema", List.of()
        )));
        when(appRepository.findById(AppKey.of("slack"))).thenReturn(Optional.of(app));

        StepSetupValidationService.SetupValidationResult result = service.validate(
                "slack", "send-message", false, null, Map.of(), Map.of(), UUID.randomUUID());

        assertFalse(result.success());
        assertTrue(result.checks().stream().anyMatch(check -> "connection".equals(check.id()) && "FAIL".equals(check.status())));
        verifyNoInteractions(connectionRepository, tokenService, resourceFetchService);
    }

    private void assertEqualsContract(StepSetupValidationService.SetupValidationResult result,
                                      String policy, boolean liveAllowed) {
        org.junit.jupiter.api.Assertions.assertEquals(policy, result.testContract().get("setupPolicy"));
        org.junit.jupiter.api.Assertions.assertEquals(liveAllowed, result.testContract().get("liveTestAllowed"));
    }
}
