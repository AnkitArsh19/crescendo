package com.crescendo.apps.approval;

import com.crescendo.logbook.workflow_run.WorkflowRun;
import com.crescendo.logbook.workflow_run.WorkflowRunRepository;
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


@RestController
@RequestMapping("/public/approvals")
public class PublicApprovalController {

    private final WorkflowRunRepository runRepo;
    private final com.crescendo.execution.suspension.WorkflowSuspensionService suspensionService;

    public PublicApprovalController(WorkflowRunRepository runRepo,
                                    com.crescendo.execution.suspension.WorkflowSuspensionService suspensionService) {
        this.runRepo = runRepo;
        this.suspensionService = suspensionService;
    }

    @GetMapping("/{token}")
    @Transactional(readOnly = true)
    public Map<String, Object> getApproval(@PathVariable String token) {
        WorkflowRun run = findRunByToken(token);
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
        Map<String, Object> responseBody = body != null ? new HashMap<>(body) : new HashMap<>();
        boolean approved = responseBody.get("approved") == null || Boolean.parseBoolean(String.valueOf(responseBody.get("approved")));
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("approved", approved);
        payload.put("approvalResponse", responseBody);
        payload.put("approvalRespondedAt", Instant.now().toString());
        payload.put("_approvalPending", false);

        boolean resumed = suspensionService.resume("approval:" + token, payload);
        if (!resumed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval not found or already submitted");
        }

        return ResponseEntity.accepted().body(Map.of(
                "accepted", true,
                "approved", approved
        ));
    }

    private WorkflowRun findRunByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval not found");
        }
        com.crescendo.execution.suspension.WorkflowSuspension suspension = suspensionService.getWaitingSuspension("approval:" + token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval not found or already submitted"));
        return runRepo.findById(suspension.getRunId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow run not found"));
    }

    private Map<String, Object> findApprovalOutput(WorkflowRun run, String token) {
        Map<String, Object> state = run.getExecutionState();
        if (state == null) {
            return Map.of();
        }
        
        try {
            for (Map.Entry<String, Object> entry : state.entrySet()) {
                if (entry.getValue() instanceof Map<?, ?> valueMap) {
                    if (token.equals(valueMap.get("approvalToken"))) {
                        Map<String, Object> result = new HashMap<>();
                        for (Map.Entry<?, ?> e : valueMap.entrySet()) {
                            result.put(String.valueOf(e.getKey()), e.getValue());
                        }
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            // ignore parsing errors
        }
        return Map.of();
    }
}
