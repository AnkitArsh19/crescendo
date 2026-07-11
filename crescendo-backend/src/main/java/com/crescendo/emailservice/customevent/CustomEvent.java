package com.crescendo.emailservice.customevent;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "custom_events",
    indexes = {
        @Index(name = "idx_custom_event_user", columnList = "userId"),
        @Index(name = "idx_custom_event_name", columnList = "name")
    })
public class CustomEvent {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "userId", nullable = false)
    private UUID userId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "jsonSchema", columnDefinition = "jsonb")
    private Map<String, Object> jsonSchema;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    public CustomEvent() {}

    public CustomEvent(UUID id, UUID userId, String name, Map<String, Object> jsonSchema) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.jsonSchema = jsonSchema;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Map<String, Object> getJsonSchema() { return jsonSchema; }
    public void setJsonSchema(Map<String, Object> jsonSchema) { this.jsonSchema = jsonSchema; }
    public Instant getCreatedAt() { return createdAt; }
}
