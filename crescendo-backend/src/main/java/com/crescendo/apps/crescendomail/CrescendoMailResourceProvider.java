package com.crescendo.apps.crescendomail;

import com.crescendo.emailservice.audience.Contact;
import com.crescendo.emailservice.audience.ContactRepository;
import com.crescendo.emailservice.domain.Domain;
import com.crescendo.emailservice.domain.DomainRepository;
import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_command;
import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_commandRepository;
import com.crescendo.execution.resource.ResourceContextDescriptor;
import com.crescendo.execution.resource.ResourceOption;
import com.crescendo.execution.resource.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

/**
 * Dynamic resource provider for CrescendoMail.
 *
 * <p>Supported resource types:
 * <ul>
 *   <li>{@code templates} — published/draft email templates
 *   <li>{@code domains}   — verified sending domains
 *   <li>{@code contacts}  — audience contacts
 * </ul>
 */
@Component
public class CrescendoMailResourceProvider implements ResourceProvider {

    private static final Logger log = LoggerFactory.getLogger(CrescendoMailResourceProvider.class);

    private final EmailTemplate_commandRepository templateRepo;
    private final DomainRepository domainRepo;
    private final ContactRepository contactRepo;

    public CrescendoMailResourceProvider() {
        this(null, null, null);
    }

    public CrescendoMailResourceProvider(EmailTemplate_commandRepository templateRepo,
                                         DomainRepository domainRepo,
                                         ContactRepository contactRepo) {
        this.templateRepo = templateRepo;
        this.domainRepo = domainRepo;
        this.contactRepo = contactRepo;
    }

    @Override
    public String appKey() {
        return "crescendomail";
    }

    @Override
    public Set<String> supportedResourceTypes() {
        return Set.of("templates", "domains", "contacts");
    }

    @Override
    public Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of(
                new ResourceContextDescriptor("templates", 50, Duration.ofMinutes(5)),
                new ResourceContextDescriptor("domains", 50, Duration.ofMinutes(5)),
                new ResourceContextDescriptor("contacts", 100, Duration.ofMinutes(5))
        );
    }

    @Override
    public List<ResourceOption> listResources(Map<String, Object> credentials,
                                               String resourceType,
                                               Map<String, String> params) {
        UUID userId = extractUserId(credentials);

        return switch (resourceType) {
            case "templates" -> listTemplates(userId);
            case "domains"   -> listDomains(userId);
            case "contacts"  -> listContacts(userId);
            default          -> List.of();
        };
    }

    private List<ResourceOption> listTemplates(UUID userId) {
        if (templateRepo == null) return List.of();
        try {
            List<EmailTemplate_command> templates = userId != null
                    ? templateRepo.findByUserIdOrderByCreatedAtDesc(userId)
                    : templateRepo.findAll();

            return templates.stream()
                    .map(t -> new ResourceOption(
                            t.getId().toString(),
                            t.getName(),
                            "Subject: " + t.getSubject() + " · " + (t.getStatus() == EmailTemplate_command.TemplateStatus.PUBLISHED ? "Published" : "Draft")
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("[crescendomail:resources] Failed to list templates: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private List<ResourceOption> listDomains(UUID userId) {
        if (domainRepo == null) return List.of();
        try {
            List<Domain> domains = userId != null
                    ? domainRepo.findByUser_IdOrderByCreatedAtDesc(userId)
                    : domainRepo.findAll();

            return domains.stream()
                    .map(d -> new ResourceOption(
                            d.getId().toString(),
                            d.getDomainName() != null ? d.getDomainName() : d.getId().toString(),
                            "Status: " + d.getStatus().name() + " · Readiness: " + d.getSendReadiness().name()
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("[crescendomail:resources] Failed to list domains: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private List<ResourceOption> listContacts(UUID userId) {
        if (contactRepo == null) return List.of();
        try {
            List<Contact> contacts = userId != null
                    ? contactRepo.findByUserIdOrderByCreatedAtDesc(userId)
                    : contactRepo.findAll();

            return contacts.stream()
                    .map(c -> {
                        String name = (c.getFirstName() != null ? c.getFirstName() + " " : "")
                                + (c.getLastName() != null ? c.getLastName() : "");
                        return new ResourceOption(
                                c.getEmail(),
                                c.getEmail(),
                                !name.isBlank() ? name.trim() + " · Subscribed: " + c.isSubscribed() : "Subscribed: " + c.isSubscribed()
                        );
                    })
                    .toList();
        } catch (Exception e) {
            log.error("[crescendomail:resources] Failed to list contacts: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private UUID extractUserId(Map<String, Object> credentials) {
        if (credentials == null) return null;
        Object raw = credentials.get("userId");
        if (raw instanceof UUID u) return u;
        if (raw instanceof String s) {
            try { return UUID.fromString(s); } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }
}
