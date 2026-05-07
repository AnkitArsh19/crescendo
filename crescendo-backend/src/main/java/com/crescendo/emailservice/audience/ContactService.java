package com.crescendo.emailservice.audience;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ContactService {

    private final ContactRepository contactRepo;

    public ContactService(ContactRepository contactRepo) {
        this.contactRepo = contactRepo;
    }

    public Contact create(UUID userId, ContactDto.CreateContactRequest req) {
        String normalized = req.email().toLowerCase().trim();
        if (contactRepo.existsByUserIdAndEmail(userId, normalized)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contact already exists: " + normalized);
        }
        Contact contact = new Contact(UUID.randomUUID(), userId, req.email(), req.firstName(), req.lastName());
        return contactRepo.save(contact);
    }

    public List<Contact> list(UUID userId) {
        return contactRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Contact get(UUID userId, UUID contactId) {
        return contactRepo.findByIdAndUserId(contactId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
    }

    public Contact update(UUID userId, UUID contactId, ContactDto.UpdateContactRequest req) {
        Contact contact = get(userId, contactId);
        if (req.firstName() != null) contact.setFirstName(req.firstName());
        if (req.lastName() != null) contact.setLastName(req.lastName());
        if (req.subscribed() != null) contact.setSubscribed(req.subscribed());
        return contactRepo.save(contact);
    }

    public void delete(UUID userId, UUID contactId) {
        Contact contact = get(userId, contactId);
        contactRepo.delete(contact);
    }

    public List<Contact> listSubscribed(UUID userId) {
        return contactRepo.findByUserIdAndSubscribedTrue(userId);
    }
}
