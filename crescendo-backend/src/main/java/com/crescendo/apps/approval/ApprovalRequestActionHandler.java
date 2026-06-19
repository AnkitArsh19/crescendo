package com.crescendo.apps.approval;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import com.crescendo.execution.action.SuspendExecutionException;
import org.springframework.beans.factory.annotation.Value;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "approval", actionKey = "request-approval")
public class ApprovalRequestActionHandler implements ActionHandler {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final String approvalBaseUrl;

    public ApprovalRequestActionHandler(
            @Value("${app.approval.base-url:${app.backend-url:http://localhost:8080}/public/approvals}") String approvalBaseUrl) {
        this.approvalBaseUrl = trimTrailingSlash(approvalBaseUrl);
    }

    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> output = new HashMap<>();
        if (context.inputData() != null) {
            output.putAll(context.inputData());
        }

        String title = asString(context.configuration().getOrDefault("title", "Approval requested"));
        String message = asString(context.configuration().getOrDefault("message", ""));
        Object fields = context.configuration().getOrDefault("fields", java.util.List.of());
        String successMessage = asString(context.configuration().getOrDefault("successMessage", "Response recorded"));

        if (context.workflowRunId() == null) {
            output.put("approvalPreview", true);
            output.put("approvalTitle", title);
            output.put("approvalMessage", message);
            output.put("approvalFields", fields);
            output.put("successMessage", successMessage);
            return ActionResult.success(output);
        }

        String token = generateToken();
        output.put("approvalUrl", approvalBaseUrl + "/" + token);
        output.put("approvalToken", token);
        output.put("approvalTitle", title);
        output.put("approvalMessage", message);
        output.put("approvalFields", fields);
        output.put("successMessage", successMessage);
        output.put("approvalRequestedAt", Instant.now().toString());
        output.put("_approvalPending", true);

        throw new SuspendExecutionException(null, token, output, "Waiting for approval response");
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String asString(Object value) {
        return value != null ? String.valueOf(value) : "";
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8080/public/approvals";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
