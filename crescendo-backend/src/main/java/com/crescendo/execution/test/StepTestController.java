package com.crescendo.execution.test;

import com.crescendo.connections.connections_command.Connections_command;
import com.crescendo.connections.connections_command.Connections_commandRepository;
import com.crescendo.connections.oauth.OAuthTokenRefreshService;
import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionHandlerRegistry;
import com.crescendo.execution.action.ActionResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

import static com.crescendo.security.AuthenticatedUser.userId;

/**
 * Workflow step setup checks and intentional live runs.
 *
 * <p>The historical {@code POST /workflows/steps/test} route remains as a
 * compatibility alias, but it is now strictly non-mutating. Production action
 * handlers can only run through {@code /live-run} after the client explicitly
 * acknowledges the side effect.</p>
 */
@RestController
@RequestMapping("/workflows/steps/test")
public class StepTestController {

    private final StepSetupValidationService setupValidationService;
    private final ActionHandlerRegistry handlerRegistry;
    private final OAuthTokenRefreshService tokenService;
    private final Connections_commandRepository connectionRepository;
    private final TriggerSampleService triggerSampleService;

    public StepTestController(StepSetupValidationService setupValidationService,
                              ActionHandlerRegistry handlerRegistry,
                              OAuthTokenRefreshService tokenService,
                              Connections_commandRepository connectionRepository,
                              TriggerSampleService triggerSampleService) {
        this.setupValidationService = setupValidationService;
        this.handlerRegistry = handlerRegistry;
        this.tokenService = tokenService;
        this.connectionRepository = connectionRepository;
        this.triggerSampleService = triggerSampleService;
    }

    public record TestStepRequest(
            String appKey,
            String actionKey,
            String triggerKey,
            Boolean isTrigger,
            String connectionId,
            Map<String, Object> configuration,
            Map<String, Object> inputData,
            Boolean acknowledgeLiveRun) {
        public boolean booleanIsTrigger() {
            return Boolean.TRUE.equals(isTrigger);
        }
        public boolean booleanAcknowledgeLiveRun() {
            return Boolean.TRUE.equals(acknowledgeLiveRun);
        }
    }

    public record TestStepResponse(
            boolean success,
            Map<String, Object> data,
            String error,
            java.util.List<StepSetupValidationService.SetupCheck> checks,
            Map<String, Object> testContract,
            String mode) {
        static TestStepResponse setup(StepSetupValidationService.SetupValidationResult result) {
            return new TestStepResponse(result.success(), result.preview(), result.error(), result.checks(),
                    result.testContract(), "SETUP_CHECK");
        }

        static TestStepResponse live(ActionResult result) {
            return new TestStepResponse(result.success(), result.outputData(), result.error(), java.util.List.of(),
                    Map.of(), "LIVE_RUN");
        }

        static TestStepResponse readSample(ActionResult result, Map<String, Object> contract) {
            return new TestStepResponse(result.success(), result.outputData(), result.error(), java.util.List.of(),
                    contract, "READ_SAMPLE");
        }

        static TestStepResponse fail(String error, String mode) {
            return new TestStepResponse(false, Map.of(), error, java.util.List.of(), Map.of(), mode);
        }
    }

    /** Backward-compatible, safe default. */
    @PostMapping
    public ResponseEntity<TestStepResponse> testStep(@RequestBody TestStepRequest request, Authentication auth) {
        return validate(request, auth);
    }

    @PostMapping("/validate")
    public ResponseEntity<TestStepResponse> validate(@RequestBody TestStepRequest request, Authentication auth) {
        String operationKey = operationKey(request);
        if (request.appKey() == null || request.appKey().isBlank() || operationKey == null || operationKey.isBlank()) {
            return ResponseEntity.badRequest().body(TestStepResponse.fail("Choose an app and operation before checking setup.", "SETUP_CHECK"));
        }
        StepSetupValidationService.SetupValidationResult result = setupValidationService.validate(
                request.appKey(), operationKey, request.booleanIsTrigger(), request.connectionId(), request.configuration(),
                request.inputData(), userId(auth));
        return ResponseEntity.ok(TestStepResponse.setup(result));
    }

    /** Deliberate production execution. This is never called by a setup check. */
    @PostMapping("/live-run")
    public ResponseEntity<TestStepResponse> runLive(@RequestBody TestStepRequest request, Authentication auth) {
        UUID userId = userId(auth);
        if (!request.booleanAcknowledgeLiveRun()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(TestStepResponse.fail("Confirm that this live run may change data in the connected app.", "LIVE_RUN"));
        }
        if (request.booleanIsTrigger()) {
            return ResponseEntity.badRequest().body(TestStepResponse.fail("Triggers use setup checks and sample records; they cannot be run as live actions.", "LIVE_RUN"));
        }
        if (request.appKey() == null || request.actionKey() == null || request.actionKey().isBlank()) {
            return ResponseEntity.badRequest().body(TestStepResponse.fail("Choose an app and action before running it live.", "LIVE_RUN"));
        }

        StepSetupValidationService.SetupValidationResult setup = setupValidationService.validate(
                request.appKey(), request.actionKey(), false, request.connectionId(), request.configuration(),
                request.inputData(), userId);
        if (!setup.success()) return ResponseEntity.badRequest().body(TestStepResponse.setup(setup));

        ActionHandler handler = handlerRegistry.find(request.appKey(), request.actionKey())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No action handler is available for this operation."));
        try {
            Map<String, Object> credentials = credentialsForExactConnection(request.appKey(), request.connectionId(), userId);
            ActionContext context = new ActionContext(request.appKey(), request.actionKey(),
                    request.configuration() == null ? Map.of() : request.configuration(), credentials,
                    request.inputData() == null ? Map.of() : request.inputData(), null, userId, null, 0);
            return ResponseEntity.ok(TestStepResponse.live(handler.execute(context)));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            return ResponseEntity.ok(TestStepResponse.fail("Live run failed: " + exception.getMessage(), "LIVE_RUN"));
        }
    }

    /**
     * Executes only a catalog-declared read-only operation to retrieve a sample
     * record. This powers actions such as listing a Spotify playlist or reading
     * a selected Google Sheet without invoking a write operation.
     */
    @PostMapping("/read-sample")
    public ResponseEntity<TestStepResponse> readSample(@RequestBody TestStepRequest request, Authentication auth) {
        UUID userId = userId(auth);
        if (request.booleanIsTrigger()) {
            return ResponseEntity.badRequest().body(TestStepResponse.fail("Trigger samples require a trigger-specific provider or captured event.", "READ_SAMPLE"));
        }
        if (request.appKey() == null || request.actionKey() == null || request.actionKey().isBlank()) {
            return ResponseEntity.badRequest().body(TestStepResponse.fail("Choose an app and read-only action first.", "READ_SAMPLE"));
        }
        StepSetupValidationService.SetupValidationResult setup = setupValidationService.validate(
                request.appKey(), request.actionKey(), false, request.connectionId(), request.configuration(),
                request.inputData(), userId);
        String policy = String.valueOf(setup.testContract().get("setupPolicy"));
        String sideEffect = String.valueOf(setup.testContract().get("sideEffect"));
        if (!setup.success()) return ResponseEntity.badRequest().body(TestStepResponse.setup(setup));
        if (!"READ_SAMPLE".equals(policy) || !"NONE".equals(sideEffect)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(TestStepResponse.fail(
                    "This operation is not declared read-only. Use Check setup or explicitly run the live action.", "READ_SAMPLE"));
        }
        ActionHandler handler = handlerRegistry.find(request.appKey(), request.actionKey())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No read handler is available for this operation."));
        try {
            Map<String, Object> credentials = credentialsForExactConnection(request.appKey(), request.connectionId(), userId);
            ActionContext context = new ActionContext(request.appKey(), request.actionKey(),
                    request.configuration() == null ? Map.of() : request.configuration(), credentials,
                    request.inputData() == null ? Map.of() : request.inputData(), null, userId, null, 0);
            return ResponseEntity.ok(TestStepResponse.readSample(handler.execute(context), setup.testContract()));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            return ResponseEntity.ok(TestStepResponse.fail("Could not fetch a read-only sample: " + exception.getMessage(), "READ_SAMPLE"));
        }
    }

    /**
     * Safely retrieves a sample trigger event or contextual mock record for downstream mapping.
     */
    @PostMapping("/trigger-sample")
    public ResponseEntity<TestStepResponse> triggerSample(@RequestBody TestStepRequest request, Authentication auth) {
        UUID userId = userId(auth);
        String triggerKey = request.triggerKey();
        if (request.appKey() == null || triggerKey == null || triggerKey.isBlank()) {
            return ResponseEntity.badRequest().body(TestStepResponse.fail("Choose an app and trigger first.", "TRIGGER_SAMPLE"));
        }
        StepSetupValidationService.SetupValidationResult setup = setupValidationService.validate(
                request.appKey(), triggerKey, true, request.connectionId(), request.configuration(),
                request.inputData(), userId);
        if (!setup.success()) return ResponseEntity.badRequest().body(TestStepResponse.setup(setup));

        try {
            Map<String, Object> sample = triggerSampleService.getTriggerSample(
                    request.appKey(), triggerKey, request.connectionId(), request.configuration(), userId);
            return ResponseEntity.ok(new TestStepResponse(true, sample, null, setup.checks(), setup.testContract(), "TRIGGER_SAMPLE"));
        } catch (Exception e) {
            return ResponseEntity.ok(TestStepResponse.fail("Could not fetch trigger sample: " + e.getMessage(), "TRIGGER_SAMPLE"));
        }
    }

    private String operationKey(TestStepRequest request) {
        return request.booleanIsTrigger() ? request.triggerKey() : request.actionKey();
    }

    private Map<String, Object> credentialsForExactConnection(String appKey, String connectionId, UUID userId) {
        if (connectionId == null || connectionId.isBlank() || "ADMIN_KEY".equalsIgnoreCase(connectionId)) return Map.of();
        UUID id;
        try {
            id = UUID.fromString(connectionId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The selected connection ID is invalid.");
        }
        Connections_command connection = connectionRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "The selected connection was not found or is not yours."));
        if (!appKey.equals(connection.getAppKey())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The selected connection belongs to another app.");
        }
        return tokenService.getValidCredentials(connection.getId(), userId);
    }
}
