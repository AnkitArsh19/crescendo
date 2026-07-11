package com.crescendo.apps.crescendomail;

import com.crescendo.emailservice.audience.Contact;
import com.crescendo.emailservice.audience.ContactDto;
import com.crescendo.emailservice.audience.ContactRepository;
import com.crescendo.emailservice.audience.ContactService;
import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

import static com.crescendo.apps.crescendomail.CrescendoMailSendHandlers.blank;
import static com.crescendo.apps.crescendomail.CrescendoMailSendHandlers.resolveUserId;

/**
 * Contact and audience action handlers for crescendomail.
 *
 * <p>Action keys:
 * <ul>
 *   <li>{@code crescendomail:upsert-contact}         — create or update a contact by email
 *   <li>{@code crescendomail:set-contact-property}   — update one property on an existing contact
 *   <li>{@code crescendomail:add-to-audience}        — set subscribed = true on a contact
 *   <li>{@code crescendomail:remove-from-audience}   — set subscribed = false on a contact
 * </ul>
 *
 * <p>Note on segments: segments are saved filter views over contact properties — there is no
 * separate "add to segment" action. Setting the property a segment filters on is what moves
 * a contact in or out of it.
 */
public class CrescendoMailContactHandlers {

    // ─────────────────────────────────────────────────────────────────────────
    // upsert-contact
    // ─────────────────────────────────────────────────────────────────────────

    @Component
    @ActionMapping(appKey = "crescendomail", actionKey = "upsert-contact")
    public static class UpsertContactHandler implements ActionHandler {

        private static final Logger log = LoggerFactory.getLogger(UpsertContactHandler.class);

        private final ContactService contactService;
        private final ContactRepository contactRepo;

        public UpsertContactHandler(ContactService contactService, ContactRepository contactRepo) {
            this.contactService = contactService;
            this.contactRepo = contactRepo;
        }

        @Override
        public ActionResult execute(ActionContext ctx) {
            String email     = ctx.getString("email");
            String firstName = ctx.getString("firstName");
            String lastName  = ctx.getString("lastName");

            if (blank(email)) return ActionResult.failure("crescendomail:upsert-contact requires 'email'");

            UUID userId = resolveUserId(ctx);
            String normalized = email.toLowerCase().trim();

            Contact contact = contactRepo.findByUserIdAndEmail(userId, normalized)
                    .map(existing -> {
                        // Update
                        ContactDto.UpdateContactRequest update = new ContactDto.UpdateContactRequest(firstName, lastName, null);
                        return contactService.update(userId, existing.getId(), update);
                    })
                    .orElseGet(() -> {
                        // Create
                        ContactDto.CreateContactRequest create = new ContactDto.CreateContactRequest(email, firstName, lastName);
                        return contactService.create(userId, create);
                    });

            log.info("[crescendomail:upsert-contact] Upserted contact {} for user {}", contact.getId(), userId);
            return ActionResult.success(contactToOutput(contact));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // set-contact-property
    // ─────────────────────────────────────────────────────────────────────────

    @Component
    @ActionMapping(appKey = "crescendomail", actionKey = "set-contact-property")
    public static class SetContactPropertyHandler implements ActionHandler {

        private static final Logger log = LoggerFactory.getLogger(SetContactPropertyHandler.class);

        private final ContactRepository contactRepo;

        public SetContactPropertyHandler(ContactRepository contactRepo) {
            this.contactRepo = contactRepo;
        }

        @Override
        public ActionResult execute(ActionContext ctx) {
            String email    = ctx.getString("email");
            String property = ctx.getString("property");
            Object value    = ctx.get("value");

            if (blank(email))    return ActionResult.failure("crescendomail:set-contact-property requires 'email'");
            if (blank(property)) return ActionResult.failure("crescendomail:set-contact-property requires 'property'");

            UUID userId = resolveUserId(ctx);
            String normalized = email.toLowerCase().trim();

            Contact contact = contactRepo.findByUserIdAndEmail(userId, normalized)
                    .orElse(null);
            if (contact == null) return ActionResult.failure("Contact not found: " + email);

            // Apply well-known properties; custom properties go to the properties map (Phase 3+)
            switch (property) {
                case "firstName"  -> contact.setFirstName(value != null ? value.toString() : null);
                case "lastName"   -> contact.setLastName(value != null ? value.toString() : null);
                case "subscribed" -> contact.setSubscribed(Boolean.parseBoolean(String.valueOf(value)));
                default -> log.warn("[crescendomail:set-contact-property] Unknown property '{}' — skipped", property);
            }
            contactRepo.save(contact);

            log.info("[crescendomail:set-contact-property] Set {}={} on contact {}", property, value, contact.getId());
            return ActionResult.success(contactToOutput(contact));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // add-to-audience
    // ─────────────────────────────────────────────────────────────────────────

    @Component
    @ActionMapping(appKey = "crescendomail", actionKey = "add-to-audience")
    public static class AddToAudienceHandler implements ActionHandler {

        private static final Logger log = LoggerFactory.getLogger(AddToAudienceHandler.class);
        private final ContactService contactService;
        private final ContactRepository contactRepo;

        public AddToAudienceHandler(ContactService contactService, ContactRepository contactRepo) {
            this.contactService = contactService;
            this.contactRepo = contactRepo;
        }

        @Override
        public ActionResult execute(ActionContext ctx) {
            String email = ctx.getString("email");
            if (blank(email)) return ActionResult.failure("crescendomail:add-to-audience requires 'email'");

            UUID userId = resolveUserId(ctx);
            String normalized = email.toLowerCase().trim();

            Contact contact = contactRepo.findByUserIdAndEmail(userId, normalized)
                    .orElseGet(() -> {
                        ContactDto.CreateContactRequest req = new ContactDto.CreateContactRequest(email, null, null);
                        return contactService.create(userId, req);
                    });

            contact.setSubscribed(true);
            contactRepo.save(contact);

            log.info("[crescendomail:add-to-audience] Subscribed {} for user {}", email, userId);
            return ActionResult.success(contactToOutput(contact));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // remove-from-audience
    // ─────────────────────────────────────────────────────────────────────────

    @Component
    @ActionMapping(appKey = "crescendomail", actionKey = "remove-from-audience")
    public static class RemoveFromAudienceHandler implements ActionHandler {

        private static final Logger log = LoggerFactory.getLogger(RemoveFromAudienceHandler.class);
        private final ContactRepository contactRepo;

        public RemoveFromAudienceHandler(ContactRepository contactRepo) {
            this.contactRepo = contactRepo;
        }

        @Override
        public ActionResult execute(ActionContext ctx) {
            String email = ctx.getString("email");
            if (blank(email)) return ActionResult.failure("crescendomail:remove-from-audience requires 'email'");

            UUID userId = resolveUserId(ctx);
            String normalized = email.toLowerCase().trim();

            contactRepo.findByUserIdAndEmail(userId, normalized).ifPresent(contact -> {
                contact.setSubscribed(false);
                contactRepo.save(contact);
            });

            log.info("[crescendomail:remove-from-audience] Unsubscribed {} for user {}", email, userId);
            return ActionResult.success(Map.of("email", email, "subscribed", false));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shared helper
    // ─────────────────────────────────────────────────────────────────────────

    static Map<String, Object> contactToOutput(Contact c) {
        return Map.of(
                "contactId",   c.getId().toString(),
                "email",       c.getEmail(),
                "firstName",   c.getFirstName() != null ? c.getFirstName() : "",
                "lastName",    c.getLastName()  != null ? c.getLastName()  : "",
                "subscribed",  c.isSubscribed()
        );
    }
}
