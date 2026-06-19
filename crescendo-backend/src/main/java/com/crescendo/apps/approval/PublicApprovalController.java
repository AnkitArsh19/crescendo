package com.crescendo.apps.approval;

import com.crescendo.config.RedisStreamConfig;
import com.crescendo.enums.WorkflowRunStatus;
import com.crescendo.logbook.domain_event.WorkflowRunCompletedEvent;
import com.crescendo.logbook.outbox.OutboxEvent;
import com.crescendo.logbook.outbox.OutboxEventRepository;
import com.crescendo.logbook.workflow_run.WorkflowRun;
import com.crescendo.logbook.workflow_run.WorkflowRunRepository;
import com.crescendo.shared.domain.event.DomainEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/public/approvals")
public class PublicApprovalController {

    private final WorkflowRunRepository runRepo;
    private final OutboxEventRepository outboxRepo;
    private final DomainEventPublisher eventPublisher;

    public PublicApprovalController(WorkflowRunRepository runRepo,
                                    OutboxEventRepository outboxRepo,
                                    DomainEventPublisher eventPublisher) {
        this.runRepo = runRepo;
        this.outboxRepo = outboxRepo;
        this.eventPublisher = eventPublisher;
    }

    @GetMapping("/{token}")
    @Transactional(readOnly = true)
    public Map<String, Object> getApproval(@PathVariable String token) {
        WorkflowRun run = findRun(token);
        Map<String, Object> approval = findApprovalOutput(run, token);
        return Map.of(
                "available", true,
                "workflowRunId", run.getId().toString(),
                "title", approval.getOrDefault("approvalTitle", "Approval requested"),
                "message", approval.getOrDefault("approvalMessage", ""),
                "fields", approval.getOrDefault("approvalFields", java.util.List.of()),
                "successMessage", approval.getOrDefault("successMessage", "Response recorded")
        );
    }

    @PostMapping("/{token}")
    @Transactional
    public ResponseEntity<Map<String, Object>> submitApproval(@PathVariable String token,
                                                              @RequestBody(required = false) Map<String, Object> body) {
        WorkflowRun run = findRun(token);
        Map<String, Object> state = run.getExecutionState() != null
                ? new HashMap<>(run.getExecutionState())
                : new HashMap<>();

        String stateKey = findApprovalStateKey(state, token);
        Map<String, Object> currentOutput = asMutableMap(state.get(stateKey));
        if (currentOutput.isEmpty()) {
            currentOutput.putAll(findApprovalOutput(run, token));
        }

        Map<String, Object> responseBody = body != null ? new HashMap<>(body) : new HashMap<>();
        boolean approved = responseBody.get("approved") == null || Boolean.parseBoolean(String.valueOf(responseBody.get("approved")));
        currentOutput.put("approved", approved);
        currentOutput.put("approvalResponse", responseBody);
        currentOutput.put("approvalRespondedAt", Instant.now().toString());
        currentOutput.put("_approvalPending", false);
        state.put(stateKey, currentOutput);

        run.setExecutionState(state);
        run.setResumeAt(null);
        run.setResumeToken(null);

        if (run.getResumeStepId() == null) {
            run.setStatus(WorkflowRunStatus.SUCCESS);
            run.setCompletedAt(Instant.now());
            eventPublisher.publish(new WorkflowRunCompletedEvent(
                    run.getId(), run.getWorkflowId(), run.getUserId(), WorkflowRunStatus.SUCCESS, null));
        } else {
            run.setStatus(WorkflowRunStatus.PENDING);
            outboxRepo.save(new OutboxEvent(
                    UUID.randomUUID(),
                    RedisStreamConfig.STREAM_EXECUTION_QUEUE,
                    buildExecutionPayload(run)
            ));
        }

        return ResponseEntity.accepted().body(Map.of(
                "accepted", true,
                "workflowRunId", run.getId().toString(),
                "approved", approved
        ));
    }

    private WorkflowRun findRun(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval not found");
        }
        return runRepo.findByResumeTokenAndStatus(token, WorkflowRunStatus.SUSPENDED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval not found or already submitted"));
    }

    private Map<String, Object> findApprovalOutput(WorkflowRun run, String token) {
        Map<String, Object> state = run.getExecutionState();
        if (state == null) {
            return Map.of();
        }
        String key = findApprovalStateKey(state, token);
        return asMutableMap(state.get(key));
    }

    private String findApprovalStateKey(Map<String, Object> state, String token) {
        for (Map.Entry<String, Object> entry : state.entrySet()) {
            Map<String, Object> value = asMutableMap(entry.getValue());
            if (token.equals(value.get("approvalToken"))) {
                return entry.getKey();
            }
        }
        return "_approval";
    }

    private Map<String, Object> asMutableMap(Object value) {
        if (value instanceof Map<?, ?> raw) {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                map.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return map;
        }
        return new HashMap<>();
    }

    private Map<String, Object> buildExecutionPayload(WorkflowRun run) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("workflowRunId", run.getId().toString());
        payload.put("workflowId", run.getWorkflowId().toString());
        payload.put("userId", run.getUserId().toString());
        return payload;
    }
}
