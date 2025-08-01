package com.crescendo.emailservice.emailtemplate.template_query;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_template_query")
public class EmailTemplate_query {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "userId", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Lob
    @Column(name = "HTMLBody", nullable = false)
    private String HTMLBody;

    @Lob
    @Column(name = "textBody")
    private String textBody;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt", nullable = false)
    private Instant updatedAt;

    public EmailTemplate_query() {
    }

    public EmailTemplate_query(UUID id, UUID userId, String name, String subject, String HTMLBody, String textBody, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.subject = subject;
        this.HTMLBody = HTMLBody;
        this.textBody = textBody;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getSubject() {
        return subject;
    }

    public String getHTMLBody() {
        return HTMLBody;
    }

    public String getTextBody() {
        return textBody;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
