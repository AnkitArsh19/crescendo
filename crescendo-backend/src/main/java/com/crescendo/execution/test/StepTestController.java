package com.crescendo.execution.test;

import com.crescendo.admin.PlatformKey;
import com.crescendo.admin.PlatformKeyRepository;
import com.crescendo.connections.connections_command.Connections_commandRepository;
import com.crescendo.connections.oauth.OAuthTokenRefreshService;
import com.crescendo.connections.security.ConnectionCredentialsCryptoService;
import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionHandlerRegistry;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static com.crescendo.security.AuthenticatedUser.userId;

import java.util.Map;
import java.util.UUID;

/**
 * Allows testing a single workflow step without creating a workflow run.
 * <p>
 * This powers the "Test" tab in the step configuration panel (like Zapier's
 * Test step).
 * Executes the action handler with user's real credentials and config, and
 * returns
 * the raw output or error.
 */
@RestController
@RequestMapping("/workflows/steps/test")
public class StepTestController {

    private static final Logger logger = LoggerFactory.getLogger(StepTestController.class);

    private final ActionHandlerRegistry handlerRegistry;
    private final Connections_commandRepository connectionsRepo;
    private final OAuthTokenRefreshService tokenService;
    private final PlatformKeyRepository platformKeyRepo;
    private final ConnectionCredentialsCryptoService cryptoService;
    private final ObjectMapper objectMapper;

    public StepTestController(ActionHandlerRegistry handlerRegistry,
            Connections_commandRepository connectionsRepo,
            OAuthTokenRefreshService tokenService,
            PlatformKeyRepository platformKeyRepo,
            ConnectionCredentialsCryptoService cryptoService,
            ObjectMapper objectMapper) {
        this.handlerRegistry = handlerRegistry;
        this.connectionsRepo = connectionsRepo;
        this.tokenService = tokenService;
        this.platformKeyRepo = platformKeyRepo;
        this.cryptoService = cryptoService;
        this.objectMapper = objectMapper;
    }

    public record TestStepRequest(
            String appKey,
            String actionKey,
            UUID connectionId,
            Map<String, Object> configuration) {
    }

    public record TestStepResponse(
            boolean success,
            Map<String, Object> data,
            String error) {
        public static TestStepResponse ok(Map<String, Object> data) {
            return new TestStepResponse(true, data, null);
        }

        public static TestStepResponse fail(String error) {
            return new TestStepResponse(false, Map.of(), error);
        }
    }

    @PostMapping
    public ResponseEntity<TestStepResponse> testStep(@RequestBody TestStepRequest request,
            Authentication auth) {
        UUID userId = userId(auth);

        // 1. Validate input
        if (request.appKey() == null || request.actionKey() == null) {
            return ResponseEntity.badRequest()
                    .body(TestStepResponse.fail("appKey and actionKey are required"));
        }

        // 2. Resolve handler
        ActionHandler handler = handlerRegistry.find(request.appKey(), request.actionKey())
                .orElse(null);

        if (handler == null) {
            // Check if this looks like a trigger key (triggers can't be tested like
            // actions)
            String key = request.actionKey();
            if (key != null && (key.startsWith("new-") || key.startsWith("updated-")
                    || key.startsWith("event-") || key.startsWith("push")
                    || key.contains("-created") || key.contains("-updated")
                    || key.contains("-cancelled") || key.contains("-completed"))) {
                return ResponseEntity.ok(TestStepResponse.ok(Map.of(
                        "provider", request.appKey(),
                        "message", "Trigger '" + key + "' is event-driven " +
                                "and will fire automatically when the event occurs. " +
                                "No test execution needed.",
                        "status", "trigger-ok")));
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No handler for " + request.appKey() + ":" + request.actionKey());
        }

        try {
            // 3. Load credentials (user connection or platform key fallback)
            Map<String, Object> credentials = Map.of();
            if (request.connectionId() != null) {
                credentials = tokenService.getValidCredentials(request.connectionId(), userId);
            }
            if (credentials.isEmpty()) {
                credentials = loadPlatformCredentials(request.appKey());
            }

            // 4. Execute with empty input data (test mode — no prior step output)
            ActionContext context = new ActionContext(
                    request.appKey(),
                    request.actionKey(),
                    request.configuration() != null ? request.configuration() : Map.of(),
                    credentials,
                    Map.of() // No input data in test mode
            );

            logger.info("[step-test] Testing {}:{} for user {}",
                    request.appKey(), request.actionKey(), userId);

            ActionResult result = handler.execute(context);

            if (result.success()) {
                return ResponseEntity.ok(TestStepResponse.ok(result.outputData()));
            } else {
                return ResponseEntity.ok(TestStepResponse.fail(result.error()));
            }

        } catch (Exception e) {
            logger.error("[step-test] Uncaught exception testing {}:{}: {}",
                    request.appKey(), request.actionKey(), e.getMessage(), e);
            return ResponseEntity.ok(
                    TestStepResponse.fail("Test failed: " + e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadPlatformCredentials(String appKey) {
        try {
            PlatformKey pk = platformKeyRepo.findByAppKeyAndEnabledTrue(appKey).orElse(null);
            if (pk != null && pk.getEncryptedCredentials() != null) {
                Map<String, Object> sealed = objectMapper
                        .readValue(pk.getEncryptedCredentials(), Map.class);
                return cryptoService.open(sealed);
            }
        } catch (Exception e) {
            logger.warn("[step-test] Failed to load platform key for '{}': {}", appKey, e.getMessage());
        }
        return Map.of();
    }
}
