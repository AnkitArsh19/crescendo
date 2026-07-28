package com.crescendo.emailservice.broadcast;

import com.crescendo.emailservice.audience.Contact;
import com.crescendo.emailservice.audience.ContactService;
import com.crescendo.emailservice.email_send.EmailSendDto;
import com.crescendo.emailservice.email_send.EmailSendService;
import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_command;
import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_commandRepository;
import com.crescendo.enums.BroadcastStatus;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BroadcastServiceTest {

    @Mock
    private BroadcastRepository broadcastRepo;
    @Mock
    private ContactService contactService;
    @Mock
    private EmailSendService emailSendService;
    @Mock
    private EmailTemplate_commandRepository templateRepo;

    private BroadcastService broadcastService;
    private final UUID userId = UUID.randomUUID();
    private final UUID templateId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        broadcastService = new BroadcastService(broadcastRepo, contactService, emailSendService, templateRepo);
    }

    @Test
    void create_templateExists_createsDraftBroadcast() {
        var req = new BroadcastDto.CreateBroadcastRequest(templateId, "newsletter@crescendo.run");
        when(templateRepo.findByIdAndUserId(templateId, userId)).thenReturn(Optional.of(new EmailTemplate_command()));
        when(broadcastRepo.save(any(Broadcast.class))).thenAnswer(inv -> inv.getArgument(0));

        Broadcast res = broadcastService.create(userId, req);

        assertThat(res.getStatus()).isEqualTo(BroadcastStatus.DRAFT);
        assertThat(res.getFromAddress()).isEqualTo("newsletter@crescendo.run");
        assertThat(res.getTemplateId()).isEqualTo(templateId);
    }

    @Test
    void send_fanOutToContacts_invokesEmailSendServiceAndCompletes() {
        UUID broadcastId = UUID.randomUUID();
        Broadcast broadcast = new Broadcast(broadcastId, userId, templateId, "newsletter@crescendo.run");
        broadcast.setStatus(BroadcastStatus.DRAFT);

        EmailTemplate_command tmpl = new EmailTemplate_command();
        tmpl.setSubject("Monthly Newsletter");

        Contact c1 = new Contact(UUID.randomUUID(), userId, "alice@example.com", "Alice", "Smith");
        Contact c2 = new Contact(UUID.randomUUID(), userId, "bob@example.com", "Bob", null);

        when(broadcastRepo.findByIdAndUserId(broadcastId, userId)).thenReturn(Optional.of(broadcast));
        when(templateRepo.findByIdAndUserId(templateId, userId)).thenReturn(Optional.of(tmpl));
        when(contactService.listSubscribed(userId)).thenReturn(List.of(c1, c2));
        when(broadcastRepo.save(any(Broadcast.class))).thenAnswer(inv -> inv.getArgument(0));

        Broadcast completed = broadcastService.send(userId, broadcastId);

        assertThat(completed.getStatus()).isEqualTo(BroadcastStatus.COMPLETED);
        assertThat(completed.getTotalCount()).isEqualTo(2);
        assertThat(completed.getSentCount()).isEqualTo(2);
        assertThat(completed.getFailedCount()).isEqualTo(0);
        assertThat(completed.getCompletedAt()).isNotNull();

        ArgumentCaptor<EmailSendDto.SendEmailRequest> captor = ArgumentCaptor.forClass(EmailSendDto.SendEmailRequest.class);
        verify(emailSendService, times(2)).sendEmail(eq(userId), eq(new UUID(0, 0)), captor.capture());
        List<EmailSendDto.SendEmailRequest> requests = captor.getAllValues();
        assertThat(requests).extracting(EmailSendDto.SendEmailRequest::to)
                .containsExactlyInAnyOrder("alice@example.com", "bob@example.com");
    }

    @Test
    void send_noSubscribedContacts_throwsBadRequest() {
        UUID broadcastId = UUID.randomUUID();
        Broadcast broadcast = new Broadcast(broadcastId, userId, templateId, "newsletter@crescendo.run");
        broadcast.setStatus(BroadcastStatus.DRAFT);

        when(broadcastRepo.findByIdAndUserId(broadcastId, userId)).thenReturn(Optional.of(broadcast));
        when(templateRepo.findByIdAndUserId(templateId, userId)).thenReturn(Optional.of(new EmailTemplate_command()));
        when(contactService.listSubscribed(userId)).thenReturn(List.of());

        assertThatThrownBy(() -> broadcastService.send(userId, broadcastId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No subscribed contacts to send to");
    }
}
