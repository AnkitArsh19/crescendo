package com.crescendo.publicapi.email;

import com.crescendo.emailservice.audience.Contact;
import com.crescendo.emailservice.audience.ContactDto;
import com.crescendo.emailservice.audience.ContactService;
import com.crescendo.publicapi.PublicApiScopes;
import com.crescendo.publicapi.common.CursorUtils;
import com.crescendo.publicapi.common.PublicPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.crescendo.security.AuthenticatedUser.userId;

@RestController
@RequestMapping("/api/v1/audiences")
@Tag(name = "Audiences (Contacts)", description = "Public API for managing contacts/audiences")
public class PublicContactController {

    private final ContactService contactService;

    public PublicContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    @Operation(summary = "Create a contact", operationId = "createContact", description = "Adds a new contact to your audience. Requires contact:write scope.")
    public ResponseEntity<ContactDto.ContactResponse> create(
            @Valid @RequestBody ContactDto.CreateContactRequest req,
            Authentication auth) {
        PublicApiScopes.require(auth, PublicApiScopes.CONTACT_WRITE);
        Contact c = contactService.create(userId(auth), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(c));
    }

    @GetMapping
    @Operation(summary = "List contacts", operationId = "listContacts", description = "Lists all contacts. Requires contact:read scope.")
    public ResponseEntity<PublicPage<ContactDto.ContactResponse>> list(
            @RequestParam(required = false, defaultValue = "100") int limit,
            @RequestParam(required = false) String after,
            Authentication auth) {
        PublicApiScopes.require(auth, PublicApiScopes.CONTACT_READ);
        
        List<ContactDto.ContactResponse> all = contactService.list(userId(auth))
                .stream().map(this::toResponse).toList();
        
        int offset = CursorUtils.decodeOffset(after);
        int toIndex = Math.min(offset + limit, all.size());
        List<ContactDto.ContactResponse> pageData = offset >= all.size() ? List.of() : all.subList(offset, toIndex);
        
        boolean hasMore = toIndex < all.size();
        String nextCursor = hasMore ? CursorUtils.encodeOffset(toIndex) : null;
        
        return ResponseEntity.ok(new PublicPage<>(pageData, hasMore, nextCursor));
    }

    @GetMapping("/{contactId}")
    @Operation(summary = "Get contact details", operationId = "getContact", description = "Gets details for a specific contact. Requires contact:read scope.")
    public ResponseEntity<ContactDto.ContactResponse> get(
            @PathVariable UUID contactId,
            Authentication auth) {
        PublicApiScopes.require(auth, PublicApiScopes.CONTACT_READ);
        return ResponseEntity.ok(toResponse(contactService.get(userId(auth), contactId)));
    }

    @PatchMapping("/{contactId}")
    @Operation(summary = "Update contact", operationId = "updateContact", description = "Updates a contact. Requires contact:write scope.")
    public ResponseEntity<ContactDto.ContactResponse> update(
            @PathVariable UUID contactId,
            @Valid @RequestBody ContactDto.UpdateContactRequest req,
            Authentication auth) {
        PublicApiScopes.require(auth, PublicApiScopes.CONTACT_WRITE);
        Contact c = contactService.update(userId(auth), contactId, req);
        return ResponseEntity.ok(toResponse(c));
    }

    @DeleteMapping("/{contactId}")
    @Operation(summary = "Delete contact", operationId = "deleteContact", description = "Deletes a contact. Requires contact:write scope.")
    public ResponseEntity<Void> delete(
            @PathVariable UUID contactId,
            Authentication auth) {
        PublicApiScopes.require(auth, PublicApiScopes.CONTACT_WRITE);
        contactService.delete(userId(auth), contactId);
        return ResponseEntity.noContent().build();
    }

    private ContactDto.ContactResponse toResponse(Contact c) {
        return new ContactDto.ContactResponse(
                c.getId(), c.getEmail(), c.getFirstName(), c.getLastName(),
                c.isSubscribed(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
