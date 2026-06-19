package com.crescendo.webhook;

import com.crescendo.execution.condition.ConditionEvaluator;
import com.crescendo.logbook.LogbookDto;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

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
        private final ObjectMapper objectMapper;

    public WebhookIngestController(WebhookRepository webhookRepo,
                                    Steps_commandRepository stepsRepo,
                                    StepConditionRepository conditionRepo,
                                    ConditionEvaluator conditionEvaluator,
                                                                        WorkflowRunService workflowRunService,
                                                                        ObjectMapper objectMapper) {
        this.webhookRepo = webhookRepo;
        this.stepsRepo = stepsRepo;
        this.conditionRepo = conditionRepo;
        this.conditionEvaluator = conditionEvaluator;
        this.workflowRunService = workflowRunService;
                this.objectMapper = objectMapper;
    }

    @PostMapping("/{webhookKey}")
    public ResponseEntity<Map<String, Object>> ingest(
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

        // 3. Evaluate trigger conditions
                Map<String, Object> safePayload = parsePayload(rawBody);
        List<StepCondition> conditions = conditionRepo.findByStepId(triggerStep.getId());

        if (!conditionEvaluator.evaluate(conditions, safePayload)) {
            logger.debug("[webhook] Conditions not met for webhook {} — ignoring", webhookKey);
            return ResponseEntity.ok(Map.of("accepted", false, "reason", "Conditions not met"));
        }

        // 4. Start workflow run — this enqueues to Redis for async execution
        workflowRunService.startRun(
                workflow.getUser().getId(),
                workflow.getId(),
                new LogbookDto.StartWorkflowRunRequest(safePayload));

        logger.info("[webhook] Accepted webhook {} → workflow {} triggered",
                webhookKey, workflow.getId());

        return ResponseEntity.accepted()
                .body(Map.of("accepted", true, "workflowId", workflow.getId().toString()));
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

        private Map<String, Object> parsePayload(String rawBody) {
                if (rawBody == null || rawBody.isBlank()) {
                        return Map.of();
                }

                try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parsed = objectMapper.readValue(rawBody, Map.class);
                        return parsed != null ? parsed : Map.of();
                } catch (Exception e) {
                        logger.warn("[webhook] Invalid JSON payload: {}", e.getMessage());
                        return Map.of();
                }
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
