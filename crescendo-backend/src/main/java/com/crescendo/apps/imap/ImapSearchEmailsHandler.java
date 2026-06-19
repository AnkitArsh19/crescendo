package com.crescendo.apps.imap;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@ActionMapping(appKey = "imap-email", actionKey = "search-emails")
public class ImapSearchEmailsHandler implements ActionHandler {

    @Override
public ActionResult execute(ActionContext context) {
        Store store = null;
        Folder folder = null;
        try {
            Properties props = new Properties();
            String host = value(context.credentials(), "host", "");
            String port = value(context.credentials(), "port", "993");
            boolean ssl = booleanValue(value(context.credentials(), "ssl", "true"));
            props.put("mail.store.protocol", ssl ? "imaps" : "imap");
            props.put("mail.imap.host", host);
            props.put("mail.imap.port", port);
            props.put("mail.imaps.host", host);
            props.put("mail.imaps.port", port);

            Session session = Session.getInstance(props);
            store = session.getStore(ssl ? "imaps" : "imap");
            store.connect(host, Integer.parseInt(port),
                    value(context.credentials(), "username", ""),
                    value(context.credentials(), "password", ""));

            folder = store.getFolder(value(context.configuration(), "folder", "INBOX"));
            folder.open(Folder.READ_ONLY);

            int limit = Math.max(1, Math.min(100, intValue(context.configuration().get("limit"), 10)));
            String subjectContains = value(context.configuration(), "subjectContains", "").toLowerCase();
            String fromContains = value(context.configuration(), "fromContains", "").toLowerCase();

            Message[] messages = folder.getMessages();
            List<Map<String, Object>> emails = new ArrayList<>();
            for (int i = messages.length - 1; i >= 0 && emails.size() < limit; i--) {
                Message message = messages[i];
                String subject = message.getSubject() != null ? message.getSubject() : "";
                String from = from(message);
                if (!subjectContains.isBlank() && !subject.toLowerCase().contains(subjectContains)) continue;
                if (!fromContains.isBlank() && !from.toLowerCase().contains(fromContains)) continue;
                emails.add(Map.of(
                        "messageNumber", message.getMessageNumber(),
                        "subject", subject,
                        "from", from,
                        "sentDate", date(message.getSentDate()),
                        "receivedDate", date(message.getReceivedDate()),
                        "preview", preview(message)
                ));
            }
            return ActionResult.success(Map.of("emails", emails, "count", emails.size()));
        } catch (Exception e) {
            return ActionResult.failure("IMAP email search failed: " + e.getMessage());
        } finally {
            try {
                if (folder != null && folder.isOpen()) folder.close(false);
                if (store != null && store.isConnected()) store.close();
            } catch (Exception ignored) {
            }
        }
    }

    private String from(Message message) throws Exception {
        if (message.getFrom() == null || message.getFrom().length == 0) return "";
        if (message.getFrom()[0] instanceof InternetAddress address) {
            return address.toUnicodeString();
        }
        return message.getFrom()[0].toString();
    }

    private String preview(Message message) throws Exception {
        Object content = message.getContent();
        String text = extractText(content);
        return text.length() > 500 ? text.substring(0, 500) : text;
    }

    private String extractText(Object content) throws Exception {
        if (content instanceof String text) {
            return text.replaceAll("\\s+", " ").trim();
        }
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
                    return extractText(part.getContent());
                }
            }
        }
        return "";
    }

    private String date(Date date) {
        return date != null ? date.toInstant().toString() : "";
    }

    private String value(Map<String, Object> map, String key, String fallback) {
        Object value = map != null ? map.get(key) : null;
        return value == null ? fallback : String.valueOf(value);
    }

    private int intValue(Object value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean booleanValue(String value) {
        return value == null || value.isBlank() || Boolean.parseBoolean(value);
    }
}
