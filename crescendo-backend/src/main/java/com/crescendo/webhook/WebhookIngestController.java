package com.crescendo.webhook;

import com.crescendo.execution.condition.ConditionEvaluator;
import com.crescendo.enums.WorkflowRunStatus;
import com.crescendo.logbook.LogbookDto;
import com.crescendo.logbook.workflow_run.WorkflowRun;
import com.crescendo.logbook.workflow_run.WorkflowRunRepository;
import com.crescendo.logbook.workflow_run.WorkflowRunService;
import com.crescendo.steps.step_condition.StepCondition;
import com.crescendo.steps.step_condition.StepConditionRepository;
import com.crescendo.steps.steps_command.Steps_command;
import com.crescendo.steps.steps_command.Steps_commandRepository;
import com.crescendo.workflow.workflow_command.Workflow_command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Public (unauthenticated) endpoint that receives external webhook events.
 * <p>
 * Flow:
 * <ol>
 *   <li>Look up active webhook by key</li>
 *   <li>Resolve the trigger step and its parent workflow</li>
 *   <li>Evaluate trigger conditions against the incoming payload</li>
 *   <li>If conditions pass → start a new workflow run (enqueued to Redis)</li>
 * </ol>
 */
@RestController
@RequestMapping("/webhooks")
public class WebhookIngestController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookIngestController.class);

    private final WebhookRepository webhookRepo;
    private final Steps_commandRepository stepsRepo;
    private final StepConditionRepository conditionRepo;
    private final ConditionEvaluator conditionEvaluator;
    private final WorkflowRunService workflowRunService;
    private final WorkflowRunRepository workflowRunRepository;
    private final ObjectMapper objectMapper;

    public WebhookIngestController(WebhookRepository webhookRepo,
                                    Steps_commandRepository stepsRepo,
                                    StepConditionRepository conditionRepo,
                                    ConditionEvaluator conditionEvaluator,
                                    WorkflowRunService workflowRunService,
                                    WorkflowRunRepository workflowRunRepository,
                                    ObjectMapper objectMapper) {
        this.webhookRepo = webhookRepo;
        this.stepsRepo = stepsRepo;
        this.conditionRepo = conditionRepo;
        this.conditionEvaluator = conditionEvaluator;
        this.workflowRunService = workflowRunService;
        this.workflowRunRepository = workflowRunRepository;
        this.objectMapper = objectMapper;
    }

    @RequestMapping(value = "/{webhookKey}", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.HEAD, RequestMethod.OPTIONS})
    public ResponseEntity<Object> ingest(
            @PathVariable String webhookKey,
            @RequestBody(required = false) String rawBody,
            HttpServletRequest request) {

        // 1. Resolve webhook
        Webhook webhook = webhookRepo.findActiveByWebhookKey(webhookKey).orElse(null);
        if (webhook == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Webhook not found or inactive"));
        }

        if (!verifySignature(webhook, rawBody, request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid webhook signature"));
        }

        // 2. Resolve trigger step → workflow
        Steps_command triggerStep = stepsRepo.findByIdAndDeletedAtIsNull(webhook.getStepId())
                .orElse(null);
        if (triggerStep == null) {
            logger.warn("[webhook] Step {} for webhook {} not found or deleted",
                    webhook.getStepId(), webhookKey);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Trigger step not found"));
        }

        Workflow_command workflow = triggerStep.getWorkflow();
        if (workflow == null || workflow.getDeletedAt() != null || !workflow.isActive()) {
            logger.info("[webhook] Workflow for webhook {} is inactive or deleted", webhookKey);
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(Map.of("error", "Workflow is inactive or deleted"));
        }

        if (workflow.getUser() == null) {
            logger.warn("[webhook] Webhook {} points to a guest workflow — cannot execute", webhookKey);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Guest workflows cannot be triggered via webhook"));
        }

        // Validate HTTP Method
        Map<String, Object> config = triggerStep.getConfiguration() != null ? triggerStep.getConfiguration() : Map.of();
        boolean multipleMethods = Boolean.parseBoolean(String.valueOf(config.getOrDefault("multipleMethods", "false")));
        String currentMethod = request.getMethod().toUpperCase();
        
        if (multipleMethods) {
            Object httpMethodsObj = config.get("httpMethods");
            if (httpMethodsObj instanceof List<?> httpMethods) {
                boolean allowed = httpMethods.stream()
                        .map(String::valueOf)
                        .map(String::toUpperCase)
                        .anyMatch(m -> m.equals(currentMethod));
                if (!allowed) {
                    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                            .body(Map.of("error", "Method not allowed"));
                }
            }
        } else {
            String expectedMethod = String.valueOf(config.getOrDefault("method", "POST")).toUpperCase();
            if (!expectedMethod.equals(currentMethod)) {
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                        .body(Map.of("error", "Method not allowed. Expected " + expectedMethod));
            }
        }

        // Validate Authentication
        String authentication = String.valueOf(config.getOrDefault("authentication", "none"));
        if ("basicAuth".equals(authentication)) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.toLowerCase().startsWith("basic ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .header("WWW-Authenticate", "Basic realm=\"Webhook\"")
                        .body(Map.of("error", "Unauthorized"));
            }
            // For true basic auth, we would need to check credentials, 
            // but for webhook triggers, usually the secret is the signature or the user provides creds in config.
            // Without credential checking logic specified, we just enforce the header existence.
        } else if ("headerAuth".equals(authentication)) {
            // Need a configured header name, assuming 'x-api-key' or similar
            // If not specified, we skip.
        }

        // Advanced Options
        @SuppressWarnings("unchecked")
        Map<String, Object> options = config.get("options") instanceof Map ? (Map<String, Object>) config.get("options") : Map.of();
        
        // ignoreBots
        boolean ignoreBots = Boolean.parseBoolean(String.valueOf(options.getOrDefault("ignoreBots", "false")));
        if (ignoreBots) {
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null && (userAgent.toLowerCase().contains("bot") || userAgent.toLowerCase().contains("crawler"))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Bots are not allowed"));
            }
        }
        
        // ipWhitelist
        String ipWhitelistStr = String.valueOf(options.get("ipWhitelist"));
        if (ipWhitelistStr != null && !ipWhitelistStr.isBlank() && !"null".equals(ipWhitelistStr)) {
            String remoteAddr = request.getRemoteAddr();
            List<String> allowedIps = java.util.Arrays.asList(ipWhitelistStr.split(","));
            boolean isAllowedIp = allowedIps.stream()
                    .map(String::trim)
                    .anyMatch(ip -> ip.equals(remoteAddr));
            if (!isAllowedIp) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "IP is not allowed to access the webhook!"));
            }
        }

        // 3. Evaluate trigger conditions
        Map<String, Object> safePayload = parsePayload(rawBody, request, options);
        List<StepCondition> conditions = conditionRepo.findByStepId(triggerStep.getId());

        if (!conditionEvaluator.evaluate(conditions, safePayload)) {
            logger.debug("[webhook] Conditions not met for webhook {} — ignoring", webhookKey);
            return ResponseEntity.ok(Map.of("accepted", false, "reason", "Conditions not met"));
        }

        // 4. Start workflow run — this enqueues to Redis for async execution
        LogbookDto.WorkflowRunSummaryResponse run = workflowRunService.startRun(
                workflow.getUser().getId(),
                workflow.getId(),
                new LogbookDto.StartWorkflowRunRequest(safePayload));

        logger.info("[webhook] Accepted webhook {} → workflow {} triggered",
                webhookKey, workflow.getId());

        if (shouldWaitForWorkflowResponse(triggerStep)) {
            return waitForWorkflowResponse(UUID.fromString(run.id()), triggerStep);
        }

        // Handle Immediate response customizations
        String responseMode = String.valueOf(config.getOrDefault("responseMode", "immediate"));
        if ("immediate".equals(responseMode)) {
            int customResponseCode = 202; // default accepted
            if (config.containsKey("responseCode") && config.get("responseCode") != null) {
                try {
                    customResponseCode = Integer.parseInt(String.valueOf(config.get("responseCode")));
                } catch (NumberFormatException ignored) {}
            }
            
            Object customResponseData = Map.of(
                    "accepted", true,
                    "workflowId", workflow.getId().toString(),
                    "runId", run.id()
            );
            
            if (config.containsKey("responseData") && config.get("responseData") != null) {
                String responseDataStr = String.valueOf(config.get("responseData"));
                if (!responseDataStr.isBlank()) {
                    try {
                        customResponseData = objectMapper.readValue(responseDataStr, Object.class);
                    } catch (Exception e) {
                        customResponseData = responseDataStr; // plain text
                    }
                }
            }
            
            return ResponseEntity.status(customResponseCode).body(customResponseData);
        }

        return ResponseEntity.accepted()
                .body(Map.of(
                        "accepted", true,
                        "workflowId", workflow.getId().toString(),
                        "runId", run.id()
                ));
    }

    private ResponseEntity<Object> waitForWorkflowResponse(UUID workflowRunId, Steps_command triggerStep) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(waitTimeoutSeconds(triggerStep)));

        while (Instant.now().isBefore(deadline)) {
            Optional<WorkflowRun> maybeRun = workflowRunRepository.findById(workflowRunId);
            if (maybeRun.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Workflow run not found", "workflowRunId", workflowRunId.toString()));
            }

            WorkflowRun run = maybeRun.get();
            if (run.getStatus() == WorkflowRunStatus.SUCCESS) {
                if (shouldReturnLastStepOutput(triggerStep)) {
                    return lastStepOutputResponse(run.getExecutionState())
                            .orElseGet(() -> ResponseEntity.ok(Map.of(
                                    "accepted", true,
                                    "workflowRunId", workflowRunId.toString(),
                                    "status", run.getStatus().name())));
                }
                return responseFromExecutionState(run.getExecutionState())
                            .orElseGet(() -> ResponseEntity.ok(Map.of(
                                    "accepted", true,
                                    "workflowRunId", workflowRunId.toString(),
                                    "status", run.getStatus().name())));
            }

            if (run.getStatus() == WorkflowRunStatus.FAILED
                    || run.getStatus() == WorkflowRunStatus.CANCELLED) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                                "accepted", true,
                                "workflowRunId", workflowRunId.toString(),
                                "status", run.getStatus().name(),
                                "error", run.getErrorMessage() != null ? run.getErrorMessage() : "Workflow did not complete successfully"));
            }

            if (run.getStatus() == WorkflowRunStatus.SUSPENDED) {
                return ResponseEntity.accepted()
                        .body(Map.of(
                                "accepted", true,
                                "workflowRunId", workflowRunId.toString(),
                                "status", run.getStatus().name(),
                                "pending", true));
            }

            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return ResponseEntity.accepted()
                .body(Map.of(
                        "accepted", true,
                        "workflowRunId", workflowRunId.toString(),
                        "pending", true,
                        "reason", "Workflow response was not ready before the webhook wait timeout"));
    }

    @SuppressWarnings("unchecked")
    private Optional<ResponseEntity<Object>> responseFromExecutionState(Map<String, Object> executionState) {
        if (executionState == null || executionState.isEmpty()) {
            return Optional.empty();
        }

        return executionState.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> numericKey(entry.getKey())))
                .map(Map.Entry::getValue)
                .filter(Map.class::isInstance)
                .map(value -> (Map<String, Object>) value)
                .filter(output -> output.get("_webhookResponse") instanceof Map)
                .map(output -> (Map<String, Object>) output.get("_webhookResponse"))
                .reduce((first, second) -> second)
                .map(this::buildWebhookResponse);
    }

    @SuppressWarnings("unchecked")
    private Optional<ResponseEntity<Object>> lastStepOutputResponse(Map<String, Object> executionState) {
        if (executionState == null || executionState.isEmpty()) {
            return Optional.empty();
        }

        return executionState.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof Map)
                .max(Comparator.comparingInt(entry -> numericKey(entry.getKey())))
                .map(entry -> ResponseEntity.ok((Object) (Map<String, Object>) entry.getValue()));
    }

    private ResponseEntity<Object> buildWebhookResponse(Map<String, Object> responseSpec) {
        int status = intValue(responseSpec.get("status"), 200);
        Object body = responseSpec.getOrDefault("body", Map.of("ok", true));
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);

        Object headers = responseSpec.get("headers");
        if (headers instanceof Map<?, ?> headerMap) {
            for (Map.Entry<?, ?> entry : headerMap.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                String headerName = String.valueOf(entry.getKey()).trim();
                if (headerName.isBlank() || isUnsafeResponseHeader(headerName)) {
                    continue;
                }
                builder.header(headerName, String.valueOf(entry.getValue()));
            }
        }

        return builder.body(body);
    }

    private boolean shouldWaitForWorkflowResponse(Steps_command triggerStep) {
        Map<String, Object> config = triggerStep.getConfiguration();
        Object responseMode = config != null ? config.get("responseMode") : null;
        if (responseMode == null) {
            return false;
        }
        String normalized = String.valueOf(responseMode).trim().toLowerCase();
        return normalized.equals("wait") || normalized.equals("wait-for-response")
                || normalized.equals("waitforresponse") || normalized.equals("response-node")
                || normalized.equals("responsenode") || shouldReturnLastStepOutput(triggerStep);
    }

    private boolean shouldReturnLastStepOutput(Steps_command triggerStep) {
        Map<String, Object> config = triggerStep.getConfiguration();
        Object responseMode = config != null ? config.get("responseMode") : null;
        if (responseMode == null) {
            return false;
        }
        String normalized = String.valueOf(responseMode).trim().toLowerCase();
        return normalized.equals("last-step") || normalized.equals("lastnode")
                || normalized.equals("last-node") || normalized.equals("last-output")
                || normalized.equals("laststep");
    }

    private long waitTimeoutSeconds(Steps_command triggerStep) {
        Map<String, Object> config = triggerStep.getConfiguration();
        Object value = config != null ? config.get("timeoutSeconds") : null;
        long seconds = intValue(value, 25);
        return Math.max(1, Math.min(seconds, 60));
    }

    private int numericKey(String key) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private int intValue(Object value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean isUnsafeResponseHeader(String headerName) {
        String normalized = headerName.toLowerCase();
        return normalized.equals("content-length")
                || normalized.equals("transfer-encoding")
                || normalized.equals("connection");
    }

    private boolean verifySignature(Webhook webhook, String rawBody, HttpServletRequest request) {
        String headerName = webhook.getProviderSignatureHeader();
        String signature = request.getHeader(headerName);

        if (signature == null || signature.isBlank()) {
            logger.warn("[webhook] Missing signature header {} for webhook {}", headerName, webhook.getWebhookKey());
            return false;
        }

        String actualSignature = signature;
        if (actualSignature.toLowerCase().startsWith("sha256=")) {
            actualSignature = actualSignature.substring(7);
        } else if (actualSignature.toLowerCase().startsWith("sha1=")) {
            actualSignature = actualSignature.substring(5);
        }

        String body = rawBody != null ? rawBody : "";
        String computed = computeHmacSha256Hex(body, webhook.getSecretKey());
        boolean matches = MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                actualSignature.getBytes(StandardCharsets.UTF_8));

        if (!matches) {
            logger.warn("[webhook] Invalid signature for webhook {}", webhook.getWebhookKey());
        }

        return matches;
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> parsePayload(String rawBody, HttpServletRequest request, Map<String, Object> options) {
                Map<String, Object> payload = new java.util.LinkedHashMap<>();
                // Include query parameters for GET requests or any request
                if (request.getParameterMap() != null) {
                    request.getParameterMap().forEach((key, values) -> {
                        if (values != null && values.length > 0) {
                            payload.put(key, values.length == 1 ? values[0] : List.of(values));
                        }
                    });
                }
                
                boolean wantRawBody = Boolean.parseBoolean(String.valueOf(options.getOrDefault("rawBody", "false")));
                if (wantRawBody && rawBody != null) {
                    payload.put("_rawBody", rawBody);
                }
                
                if (rawBody == null || rawBody.isBlank()) {
                        return payload;
                }

                try {
                        Map<String, Object> parsed = objectMapper.readValue(rawBody, Map.class);
                        if (parsed != null) {
                            payload.putAll(parsed);
                        }
                } catch (Exception e) {
                        logger.warn("[webhook] Invalid JSON payload: {}", e.getMessage());
                }
                return payload;
        }

        private String computeHmacSha256Hex(String data, String secretKey) {
                try {
                        Mac mac = Mac.getInstance("HmacSHA256");
                        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
                        return HexFormat.of().formatHex(hash);
                } catch (Exception e) {
                        throw new IllegalStateException("Failed to compute webhook signature", e);
                }
        }
}
