package com.crescendo.emailservice.audience;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.crescendo.security.AuthenticatedUser.userId;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/settings/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    public ResponseEntity<ContactDto.ContactResponse> create(
            @Valid @RequestBody ContactDto.CreateContactRequest req,
            Authentication auth) {
        Contact c = contactService.create(userId(auth), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(c));
    }

    @GetMapping
    public ResponseEntity<List<ContactDto.ContactResponse>> list(Authentication auth) {
        List<ContactDto.ContactResponse> result = contactService.list(userId(auth))
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{contactId}")
    public ResponseEntity<ContactDto.ContactResponse> get(@PathVariable UUID contactId,
                                                          Authentication auth) {
        return ResponseEntity.ok(toResponse(contactService.get(userId(auth), contactId)));
    }

    @PatchMapping("/{contactId}")
    public ResponseEntity<ContactDto.ContactResponse> update(
            @PathVariable UUID contactId,
            @Valid @RequestBody ContactDto.UpdateContactRequest req,
            Authentication auth) {
        Contact c = contactService.update(userId(auth), contactId, req);
        return ResponseEntity.ok(toResponse(c));
    }

    @DeleteMapping("/{contactId}")
    public ResponseEntity<Void> delete(@PathVariable UUID contactId, Authentication auth) {
        contactService.delete(userId(auth), contactId);
        return ResponseEntity.noContent().build();
    }

    private ContactDto.ContactResponse toResponse(Contact c) {
        return new ContactDto.ContactResponse(
                c.getId(), c.getEmail(), c.getFirstName(), c.getLastName(),
                c.isSubscribed(), c.getCreatedAt(), c.getUpdatedAt());
    }

}
