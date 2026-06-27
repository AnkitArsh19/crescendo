package com.crescendo.apps.brevo;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.utils.RestClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BrevoEmailHandlers {

    private String getBaseUrl() {
        return "https://api.brevo.com/v3";
    }

    @ActionMapping(appKey = "brevo", actionKey = "brevo:email:send")
    public Object sendEmail(ActionContext context) throws Exception {
        String senderEmail = context.getString("senderEmail");
        String senderName = context.getString("senderName");
        String recipientEmail = context.getString("recipientEmail");
        String subject = context.getString("subject");
        String htmlContent = context.getString("htmlContent");

        Map<String, Object> sender = new HashMap<>();
        sender.put("email", senderEmail);
        if (senderName != null) sender.put("name", senderName);

        Map<String, Object> to = Map.of("email", recipientEmail);

        Map<String, Object> body = Map.of(
                "sender", sender,
                "to", List.of(to),
                "subject", subject,
                "htmlContent", htmlContent
        );

        return RestClient.builder()
                .url(getBaseUrl() + "/smtp/email")
                .header("api-key", context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }

    @ActionMapping(appKey = "brevo", actionKey = "brevo:email:sendTemplate")
    public Object sendTemplate(ActionContext context) throws Exception {
        Integer templateId = context.getInt("templateId");
        String recipientEmail = context.getString("recipientEmail");

        Map<String, Object> to = Map.of("email", recipientEmail);

        Map<String, Object> body = Map.of(
                "templateId", templateId,
                "to", List.of(to)
        );

        return RestClient.builder()
                .url(getBaseUrl() + "/smtp/email")
                .header("api-key", context.getCredential("apiKey"))
                .header("Content-Type", "application/json")
                .post(body)
                .execute();
    }
}
