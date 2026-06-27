package com.crescendo.apps.imap;

import com.crescendo.execution.trigger.TriggerPoller;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.Multipart;
import jakarta.mail.BodyPart;
import jakarta.mail.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
// import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Properties;

@Component
public class ImapEmailTriggerPoller implements TriggerPoller {

    private static final Logger logger = LoggerFactory.getLogger(ImapEmailTriggerPoller.class);

    @Override
    public boolean supports(String appKey, String triggerKey) {
        return "imap-email".equals(appKey) && "email-received".equals(triggerKey);
    }

    @Override
    public List<Map<String, Object>> poll(Map<String, Object> credentials, Map<String, Object> configuration, Instant lastPollTime) {
        List<Map<String, Object>> events = new ArrayList<>();
        if (credentials == null || configuration == null) return events;

        Store store = null;
        Folder folder = null;

        try {
            Properties props = new Properties();
            String host = value(credentials, "host", "");
            String port = value(credentials, "port", "993");
            boolean ssl = booleanValue(value(credentials, "ssl", "true"));
            props.put("mail.store.protocol", ssl ? "imaps" : "imap");
            props.put("mail.imap.host", host);
            props.put("mail.imap.port", port);
            props.put("mail.imaps.host", host);
            props.put("mail.imaps.port", port);

            Session session = Session.getInstance(props);
            store = session.getStore(ssl ? "imaps" : "imap");
            store.connect(host, Integer.parseInt(port),
                    value(credentials, "username", ""),
                    value(credentials, "password", ""));

            String mailbox = value(configuration, "mailbox", "INBOX");
            folder = store.getFolder(mailbox);
            
            String postProcessAction = value(configuration, "postProcessAction", "read");
            folder.open("read".equals(postProcessAction) ? Folder.READ_WRITE : Folder.READ_ONLY);

            boolean trackLastMessageId = booleanValue(value(configuration, "trackLastMessageId", "true"));
            String customEmailConfig = value(configuration, "customEmailConfig", "[\"UNSEEN\"]");

            List<SearchTerm> terms = new ArrayList<>();

            if (customEmailConfig.contains("UNSEEN")) {
                terms.add(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            }

            if (trackLastMessageId && lastPollTime != null) {
                terms.add(new ReceivedDateTerm(ComparisonTerm.GE, Date.from(lastPollTime)));
            }

            SearchTerm finalTerm = null;
            if (terms.size() == 1) {
                finalTerm = terms.get(0);
            } else if (terms.size() > 1) {
                finalTerm = new AndTerm(terms.toArray(new SearchTerm[0]));
            }

            Message[] messages = finalTerm != null ? folder.search(finalTerm) : folder.getMessages();

            boolean downloadAttachments = booleanValue(value(configuration, "downloadAttachments", "false"));
            String format = value(configuration, "format", "simple");
            String prefix = value(configuration, "dataPropertyAttachmentsPrefixName", "attachment_");

            for (Message message : messages) {
                Map<String, Object> payload = new LinkedHashMap<>();
                
                String subject = message.getSubject() != null ? message.getSubject() : "";
                String from = from(message);
                
                payload.put("subject", subject);
                payload.put("from", from);
                payload.put("date", message.getReceivedDate() != null ? message.getReceivedDate().toInstant().toString() : "");

                List<Map<String, String>> attachmentsList = new ArrayList<>();
                StringBuilder textBuilder = new StringBuilder();
                extractTextAndAttachments(message.getContent(), downloadAttachments || "resolved".equals(format), attachmentsList, textBuilder);

                if ("resolved".equals(format) || downloadAttachments) {
                    for (int i = 0; i < attachmentsList.size(); i++) {
                        payload.put(prefix + i, attachmentsList.get(i));
                    }
                }

                if ("raw".equals(format)) {
                    // Simulating raw format
                    payload.put("raw", Base64.getEncoder().encodeToString(textBuilder.toString().getBytes()));
                } else {
                    payload.put("text", textBuilder.toString());
                }

                events.add(payload);

                if ("read".equals(postProcessAction) && !message.isSet(Flags.Flag.SEEN)) {
                    message.setFlag(Flags.Flag.SEEN, true);
                }
            }

        } catch (Exception e) {
            logger.error("[imap-poller] Failed to poll IMAP folder", e);
        } finally {
            try {
                if (folder != null && folder.isOpen()) folder.close(false);
                if (store != null && store.isConnected()) store.close();
            } catch (Exception ignored) {
            }
        }

        return events;
    }

    private String from(Message message) throws Exception {
        if (message.getFrom() == null || message.getFrom().length == 0) return "";
        if (message.getFrom()[0] instanceof InternetAddress address) {
            return address.toUnicodeString();
        }
        return message.getFrom()[0].toString();
    }

    private void extractTextAndAttachments(Object content, boolean downloadAttachments, List<Map<String, String>> attachmentsList, StringBuilder textBuilder) throws Exception {
        if (content instanceof String text) {
            textBuilder.append(text).append("\n");
            return;
        }
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart part = multipart.getBodyPart(i);
                String disposition = part.getDisposition();
                if ((disposition != null && (disposition.equalsIgnoreCase(Part.ATTACHMENT) || disposition.equalsIgnoreCase(Part.INLINE))) || part.getFileName() != null) {
                    if (downloadAttachments && part.getFileName() != null) {
                        try (InputStream is = part.getInputStream()) {
                            byte[] bytes = is.readAllBytes();
                            String base64 = Base64.getEncoder().encodeToString(bytes);
                            attachmentsList.add(Map.of(
                                "fileName", part.getFileName(),
                                "contentType", part.getContentType(),
                                "base64", base64
                            ));
                        }
                    }
                } else if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
                    extractTextAndAttachments(part.getContent(), downloadAttachments, attachmentsList, textBuilder);
                } else if (part.isMimeType("multipart/*")) {
                    extractTextAndAttachments(part.getContent(), downloadAttachments, attachmentsList, textBuilder);
                }
            }
        }
    }

    private String value(Map<String, Object> map, String key, String fallback) {
        Object value = map != null ? map.get(key) : null;
        return value == null ? fallback : String.valueOf(value);
    }

    private boolean booleanValue(String value) {
        if (value == null || value.isBlank()) return false;
        return Boolean.parseBoolean(value);
    }
}
