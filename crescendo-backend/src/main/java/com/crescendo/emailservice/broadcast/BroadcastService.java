package com.crescendo.emailservice.broadcast;

import com.crescendo.emailservice.audience.Contact;
import com.crescendo.emailservice.audience.ContactService;
import com.crescendo.emailservice.email_send.EmailSendDto;
import com.crescendo.emailservice.email_send.EmailSendService;
import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_command;
import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_commandRepository;
import com.crescendo.enums.BroadcastStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages broadcast lifecycle: create (DRAFT) → send (fan-out to contacts).
 *
 * Fan-out delegates to EmailSendService per contact, reusing the full pipeline
 * (suppression check, tracking injection, template interpolation, Redis enqueue).
 */
@Service
public class BroadcastService {

    private static final Logger logger = LoggerFactory.getLogger(BroadcastService.class);
    private static final UUID DASHBOARD_API_KEY_ID = new UUID(0, 0);

    private final BroadcastRepository broadcastRepo;
    private final ContactService contactService;
    private final EmailSendService emailSendService;
    private final EmailTemplate_commandRepository templateRepo;

    public BroadcastService(BroadcastRepository broadcastRepo,
                            ContactService contactService,
                            EmailSendService emailSendService,
                            EmailTemplate_commandRepository templateRepo) {
        this.broadcastRepo = broadcastRepo;
        this.contactService = contactService;
        this.emailSendService = emailSendService;
        this.templateRepo = templateRepo;
    }

    public Broadcast create(UUID userId, BroadcastDto.CreateBroadcastRequest req) {
        // Verify template exists
        templateRepo.findByIdAndUserId(req.templateId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Template not found: " + req.templateId()));

        Broadcast broadcast = new Broadcast(UUID.randomUUID(), userId, req.templateId(), req.fromAddress());
        return broadcastRepo.save(broadcast);
    }

    public List<Broadcast> list(UUID userId) {
        return broadcastRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Broadcast get(UUID userId, UUID broadcastId) {
        return broadcastRepo.findByIdAndUserId(broadcastId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Broadcast not found"));
    }

    /**
     * Fan-out: sends the broadcast template to every subscribed contact.
     * Each email goes through the full EmailSendService pipeline (suppression,
     * tracking, template interpolation) and is enqueued to Redis for async delivery.
     */
    public Broadcast send(UUID userId, UUID broadcastId) {
        Broadcast broadcast = get(userId, broadcastId);

        if (broadcast.getStatus() != BroadcastStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Broadcast can only be sent from DRAFT status (current: " + broadcast.getStatus() + ")");
        }

        // Verify template still exists
        EmailTemplate_command template = templateRepo.findByIdAndUserId(broadcast.getTemplateId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Template no longer exists: " + broadcast.getTemplateId()));

        List<Contact> contacts = contactService.listSubscribed(userId);
        if (contacts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No subscribed contacts to send to");
        }

        broadcast.setStatus(BroadcastStatus.SENDING);
        broadcast.setStartedAt(Instant.now());
        broadcast.setTotalCount(contacts.size());
        broadcastRepo.save(broadcast);

        int sentCount = 0;
        int failedCount = 0;

        for (Contact contact : contacts) {
            try {
                Map<String, Object> templateData = new HashMap<>();
                templateData.put("firstName", contact.getFirstName() != null ? contact.getFirstName() : "");
                templateData.put("lastName", contact.getLastName() != null ? contact.getLastName() : "");
                templateData.put("email", contact.getEmail());

                var req = new EmailSendDto.SendEmailRequest(
                        broadcast.getFromAddress(),
                        contact.getEmail(),
                        template.getSubject(),   // placeholder — template overrides
                        null, null,
                        broadcast.getTemplateId(),
                        templateData,
                        com.crescendo.enums.EmailType.MARKETING
                );

                emailSendService.sendEmail(userId, DASHBOARD_API_KEY_ID, req);
                sentCount++;
            } catch (Exception e) {
                failedCount++;
                logger.warn("[broadcast] Failed for contact {} in broadcast {}: {}",
                        contact.getEmail(), broadcastId, e.getMessage());
            }
        }

        broadcast.setSentCount(sentCount);
        broadcast.setFailedCount(failedCount);
        broadcast.setCompletedAt(Instant.now());
        broadcast.setStatus(failedCount == contacts.size() ? BroadcastStatus.FAILED : BroadcastStatus.COMPLETED);
        return broadcastRepo.save(broadcast);
    }

    public void delete(UUID userId, UUID broadcastId) {
        Broadcast broadcast = get(userId, broadcastId);
        if (broadcast.getStatus() == BroadcastStatus.SENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete a broadcast that is currently sending");
        }
        broadcastRepo.delete(broadcast);
    }
}
