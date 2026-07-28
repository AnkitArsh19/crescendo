package com.crescendo.emailservice.audience;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepo;

    private ContactService contactService;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        contactService = new ContactService(contactRepo);
    }

    @Test
    void create_newContact_normalizesEmailAndSaves() {
        var req = new ContactDto.CreateContactRequest("  TEST.User@example.COM ", "John", "Doe");
        when(contactRepo.existsByUserIdAndEmail(userId, "test.user@example.com")).thenReturn(false);
        when(contactRepo.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        Contact contact = contactService.create(userId, req);

        assertThat(contact.getEmail()).isEqualTo("test.user@example.com");
        ArgumentCaptor<Contact> captor = ArgumentCaptor.forClass(Contact.class);
        verify(contactRepo).save(captor.capture());
        assertThat(captor.getValue().getFirstName()).isEqualTo("John");
        assertThat(captor.getValue().getLastName()).isEqualTo("Doe");
        assertThat(captor.getValue().isSubscribed()).isTrue();
    }

    @Test
    void create_duplicateEmail_throwsConflict() {
        var req = new ContactDto.CreateContactRequest("duplicate@example.com", "Jane", "Doe");
        when(contactRepo.existsByUserIdAndEmail(userId, "duplicate@example.com")).thenReturn(true);

        assertThatThrownBy(() -> contactService.create(userId, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Contact already exists");
    }

    @Test
    void update_changesSubscriptionStatus() {
        UUID contactId = UUID.randomUUID();
        Contact existing = new Contact(contactId, userId, "user@example.com", "John", "Doe");
        when(contactRepo.findByIdAndUserId(contactId, userId)).thenReturn(Optional.of(existing));
        when(contactRepo.save(any(Contact.class))).thenAnswer(inv -> inv.getArgument(0));

        var updateReq = new ContactDto.UpdateContactRequest(null, "Smith", false);
        Contact updated = contactService.update(userId, contactId, updateReq);

        assertThat(updated.getLastName()).isEqualTo("Smith");
        assertThat(updated.getFirstName()).isEqualTo("John");
        assertThat(updated.isSubscribed()).isFalse();
    }

    @Test
    void listSubscribed_returnsOnlyActiveSubscribers() {
        Contact sub1 = new Contact(UUID.randomUUID(), userId, "sub1@test.com", "A", "B");
        Contact sub2 = new Contact(UUID.randomUUID(), userId, "sub2@test.com", "C", "D");
        when(contactRepo.findByUserIdAndSubscribedTrue(userId)).thenReturn(List.of(sub1, sub2));

        List<Contact> subs = contactService.listSubscribed(userId);

        assertThat(subs).hasSize(2);
        assertThat(subs).extracting(Contact::getEmail).containsExactly("sub1@test.com", "sub2@test.com");
    }
}
