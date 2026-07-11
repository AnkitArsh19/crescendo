package com.crescendo.emailservice.emailtemplate.template_command;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "email_template_command",
    indexes = {
        @Index(name = "idx_email_template_user", columnList = "userId")
    })
public class EmailTemplate_command {

    // ── Lifecycle status ────────────────────────────────────────────────────
    public enum TemplateStatus { DRAFT, PUBLISHED }

    // ── Variable type enum ──────────────────────────────────────────────────
    public enum VariableType { STRING, NUMBER }

    /**
     * A single template variable — resolved at send time.
     * Reserved names: FIRST_NAME, LAST_NAME, EMAIL, CRESCENDO_UNSUBSCRIBE_URL.
     */
    public record TemplateVariable(
            String name,
            VariableType type,
            String fallbackValue  // nullable — if null, value MUST be supplied at send time
    ) {}

    /**
     * Frozen copy of the template content captured at publish time.
     * Guarantees that later draft edits don't retroactively change emails already sent.
     */
    public record PublishedSnapshot(
            String subject,
            String htmlBody,
            String textBody,
            List<TemplateVariable> variables,
            Instant publishedAt
    ) {}

    // ── Fields ──────────────────────────────────────────────────────────────

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "userId", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "subject", nullable = false, length = 1000)
    private String subject;

    @Lob
    @Column(name = "HTMLBody", nullable = false)
    private String HTMLBody;

    @Lob
    @Column(name = "textBody")
    private String textBody;

    /** Draft/Published lifecycle. All templates start as DRAFT. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TemplateStatus status = TemplateStatus.DRAFT;

    /**
     * List of variables declared for this template.
     * Each {{VAR}} reference in subject/body must have a matching entry here (enforced on publish).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variables", columnDefinition = "jsonb")
    private List<TemplateVariable> variables = new ArrayList<>();

    /**
     * Snapshot frozen at publish time — what was actually used for sends.
     * Null until first publish. Survives subsequent draft edits unchanged.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "publishedVersionSnapshot", columnDefinition = "jsonb")
    private PublishedSnapshot publishedVersionSnapshot;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt", nullable = false)
    private Instant updatedAt;

    public EmailTemplate_command() {}

    public EmailTemplate_command(UUID id, UUID userId, String name, String subject, String HTMLBody, String textBody) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.subject = subject;
        this.HTMLBody = HTMLBody;
        this.textBody = textBody;
        this.status = TemplateStatus.DRAFT;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getHTMLBody() { return HTMLBody; }
    public void setHTMLBody(String HTMLBody) { this.HTMLBody = HTMLBody; }

    public String getTextBody() { return textBody; }
    public void setTextBody(String textBody) { this.textBody = textBody; }

    public TemplateStatus getStatus() { return status; }
    public void setStatus(TemplateStatus status) { this.status = status; }

    public List<TemplateVariable> getVariables() { return variables; }
    public void setVariables(List<TemplateVariable> variables) { this.variables = variables != null ? variables : new ArrayList<>(); }

    public PublishedSnapshot getPublishedVersionSnapshot() { return publishedVersionSnapshot; }
    public void setPublishedVersionSnapshot(PublishedSnapshot snap) { this.publishedVersionSnapshot = snap; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
