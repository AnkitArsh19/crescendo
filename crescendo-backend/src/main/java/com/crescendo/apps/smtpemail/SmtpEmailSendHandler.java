package com.crescendo.apps.smtpemail;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@ActionMapping(appKey = "smtp-email", actionKey = "send-email")
public class SmtpEmailSendHandler implements ActionHandler {
    private final ObjectMapper mapper;

    public SmtpEmailSendHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
public ActionResult execute(ActionContext c) {
        try {
            String to = cfg(c, "to");
            String from = !cfg(c, "from").isBlank() ? cfg(c, "from") : cred(c, "defaultFrom");
            if (to.isBlank()) {
                return ActionResult.failure("SMTP email requires 'to'");
            }
            if (from.isBlank()) {
                return ActionResult.failure("SMTP email requires 'from' or a defaultFrom credential");
            }
            if (cfg(c, "subject").isBlank()) {
                return ActionResult.failure("SMTP email requires 'subject'");
            }
            if (cfg(c, "htmlBody").isBlank() && cfg(c, "textBody").isBlank()) {
                return ActionResult.failure("SMTP email requires htmlBody or textBody");
            }

            JavaMailSenderImpl mailSender = sender(c);
            var mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setTo(addresses(to));
            if (!cfg(c, "cc").isBlank()) {
                helper.setCc(addresses(cfg(c, "cc")));
            }
            if (!cfg(c, "bcc").isBlank()) {
                helper.setBcc(addresses(cfg(c, "bcc")));
            }
            helper.setFrom(from);
            helper.setSubject(cfg(c, "subject"));
            if (!cfg(c, "htmlBody").isBlank()) {
                helper.setText(cfg(c, "textBody"), cfg(c, "htmlBody"));
            } else {
                helper.setText(cfg(c, "textBody"), false);
            }

            List<Map<String, Object>> attachments = attachments(c);
            for (Map<String, Object> attachment : attachments) {
                String filename = str(attachment.get("filename"));
                String base64 = str(attachment.get("base64"));
                if (filename.isBlank() || base64.isBlank()) {
                    continue;
                }
                byte[] bytes = Base64.getDecoder().decode(base64);
                String contentType = str(attachment.get("contentType"));
                if (contentType.isBlank()) {
                    helper.addAttachment(filename, new ByteArrayResource(bytes));
                } else {
                    helper.addAttachment(filename, new ByteArrayResource(bytes), contentType);
                }
            }

            mailSender.send(mime);
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("sent", true);
            output.put("to", java.util.Arrays.asList(addresses(to)));
            output.put("from", from);
            output.put("subject", cfg(c, "subject"));
            output.put("attachments", attachments.size());
            output.put("messageId", mime.getMessageID());
            return ActionResult.success(output);
        } catch (Exception e) {
            return ActionResult.failure("SMTP email failed: " + e.getMessage());
        }
    }

    private JavaMailSenderImpl sender(ActionContext c) {
        JavaMailSenderImpl s = new JavaMailSenderImpl();
        s.setHost(cred(c, "host"));
        s.setPort(intVal(cred(c, "port"), 587));
        if (!cred(c, "username").isBlank()) {
            s.setUsername(cred(c, "username"));
        }
        if (!cred(c, "password").isBlank()) {
            s.setPassword(cred(c, "password"));
        }
        Properties props = s.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(boolCred(c, "auth", !cred(c, "username").isBlank())));
        props.put("mail.smtp.starttls.enable", String.valueOf(boolCred(c, "startTls", true)));
        props.put("mail.smtp.ssl.enable", String.valueOf(boolCred(c, "ssl", false)));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "30000");
        props.put("mail.smtp.writetimeout", "30000");
        return s;
    }

    private List<Map<String, Object>> attachments(ActionContext c) throws Exception {
        String raw = cfg(c, "attachmentsJson");
        if (raw.isBlank()) {
            return List.of();
        }
        Object parsed = mapper.readValue(raw, Object.class);
        if (!(parsed instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> normalized = new LinkedHashMap<>();
                map.forEach((k, v) -> normalized.put(String.valueOf(k), v));
                out.add(normalized);
            }
        }
        return out;
    }

    private String[] addresses(String csv) {
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
    }

    private boolean boolCred(ActionContext c, String key, boolean fallback) {
        String value = cred(c, key);
        return value.isBlank() ? fallback : Boolean.parseBoolean(value);
    }

    private int intVal(String value, int fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String cfg(ActionContext c, String key) {
        Object value = c.configuration().get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private String cred(ActionContext c, String key) {
        Object value = c.credentials() != null ? c.credentials().get(key) : null;
        return value == null ? "" : String.valueOf(value);
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
