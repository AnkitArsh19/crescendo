package com.crescendo.apps.brevo;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@ActionMapping(appKey = "brevo", actionKey = "send-email")
public class BrevoSendEmailHandler extends BrevoHandler {

    public BrevoSendEmailHandler(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
public ActionResult execute(ActionContext context) {
        String to = value(context, "to", "");
        String subject = value(context, "subject", "");
        String htmlContent = value(context, "htmlContent", "");
        String senderEmail = value(context, "senderEmail", "");
        if (to.isBlank() || subject.isBlank() || htmlContent.isBlank() || senderEmail.isBlank()) {
            return ActionResult.failure("Brevo to, subject, htmlContent, and senderEmail are required");
        }
        return post(context, "/smtp/email", Map.of(
                "sender", Map.of("email", senderEmail, "name", value(context, "senderName", "")),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", htmlContent
        ));
    }
}
