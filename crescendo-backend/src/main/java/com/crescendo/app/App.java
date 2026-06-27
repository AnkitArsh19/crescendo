package com.crescendo.app;

import com.crescendo.enums.AuthType;
import com.crescendo.shared.domain.valueobject.AppKey;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

/**
 * Entity representing an application/integration (e.g., Gmail, Slack, Notion).
 * Uses AppKey value object for the primary key with validation.
 *
 * <p>New fields added for the connections UI:
 * <ul>
 *   <li>{@code credentialSchema} — JSON array defining what credential fields
 *       the user must fill in when creating a connection. Empty for OAuth apps
 *       (the credentials are obtained automatically via OAuth callback).</li>
 *   <li>{@code category} — grouping for the UI (communication, developer, ai, etc.)</li>
 *   <li>{@code helpUrl} — link to the provider's developer dashboard</li>
 *   <li>{@code internal} — if true the app is hidden from the user-facing catalog</li>
 *   <li>{@code altAuthType} — optional secondary auth type (e.g. Discord supports
 *       both APIKEY and OAUTH2)</li>
 * </ul>
 */
@Entity
@Table(name = "app",
    indexes = {
        @Index(name = "idx_app_name", columnList = "name")
    })
public class App {

    @Id
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "appKey", nullable = false, length = 100))
    private AppKey appKey;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Rich Markdown description shown in the AppSetupGuide Overview tab.
     * Single source of truth — stored in DB, returned by the API.
     * Supports bold, bullets, headers, and inline code.
     * No character limit — typical descriptions are 500–3,000 chars.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "logoUrl", length = 500)
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "authType", nullable = false, length = 30)
    private AuthType authType;

    /** Optional secondary auth type (e.g. Discord supports bot-token AND OAuth). */
    @Enumerated(EnumType.STRING)
    @Column(name = "altAuthType", length = 30)
    private AuthType altAuthType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "triggers", columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> triggers;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actions", columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> actions;

    /**
     * JSON array describing the credential fields presented to the user
     * when they create a connection for this app.
     * <p>Example entry:
     * <pre>{"key":"apiKey","label":"API Key","type":"password","required":true,
     *       "placeholder":"sk-...","helpText":"Get your key at ..."}</pre>
     * Empty for OAUTH2-only apps (credentials are obtained via the OAuth callback).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "credentialSchema", columnDefinition = "jsonb")
    private List<Map<String, Object>> credentialSchema;

    /** UI grouping: communication, developer, ai, productivity, payments, fun, internal. */
    @Column(name = "category", length = 50)
    private String category;

    /** Link to the provider's developer dashboard / key-generation page. */
    @Column(name = "helpUrl", length = 500)
    private String helpUrl;

    /** Internal apps (e.g. debug log) are hidden from the user-facing app catalog. */
    @Column(name = "internal", nullable = false)
    private boolean internal = false;

    /**
     * Transient — NOT persisted. Populated at query time by {@link AppService}
     * by checking whether an enabled {@code PlatformKey} exists for this app.
     *
     * <p>When {@code true}, the frontend shows a "Use Crescendo's Key" toggle so
     * the user can run the app without providing personal credentials.
     * When {@code false}, the user must connect their own account or provide their own key.
     */
    @Transient
    private boolean hasPlatformKey = false;

    // ─── Constructors ──────────────────────────────────────────────

    public App() {}

    /** Legacy 7-arg constructor — kept for backward compatibility. */
    public App(AppKey appKey, String name, String description, String logoUrl, AuthType authType,
               List<Map<String, Object>> triggers, List<Map<String, Object>> actions) {
        this.appKey = appKey;
        this.name = name;
        this.description = description;
        this.logoUrl = logoUrl;
        this.authType = authType;
        this.triggers = triggers;
        this.actions = actions;
    }

    /** Convenience legacy constructor accepting raw string for appKey. */
    public App(String appKey, String name, String description, String logoUrl, AuthType authType,
               List<Map<String, Object>> triggers, List<Map<String, Object>> actions) {
        this(AppKey.of(appKey), name, description, logoUrl, authType, triggers, actions);
    }

    // ─── Fluent Setters (builder-style for the new fields) ─────────

    public App credentialSchema(List<Map<String, Object>> schema) {
        this.credentialSchema = schema;
        return this;
    }

    public App category(String category) {
        this.category = category;
        return this;
    }

    public App helpUrl(String helpUrl) {
        this.helpUrl = helpUrl;
        return this;
    }

    public App internal(boolean internal) {
        this.internal = internal;
        return this;
    }

    public App altAuthType(AuthType altAuthType) {
        this.altAuthType = altAuthType;
        return this;
    }

    public App hasPlatformKey(boolean hasPlatformKey) {
        this.hasPlatformKey = hasPlatformKey;
        return this;
    }

    // ─── Getters / Setters ─────────────────────────────────────────

    public AppKey getAppKeyVO() { return appKey; }

    /** Returns raw app key string for compatibility. */
    public String getAppKey() { return appKey != null ? appKey.value() : null; }

    public void setAppKey(AppKey appKey) { this.appKey = appKey; }
    public void setAppKey(String appKey) { this.appKey = AppKey.of(appKey); }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public AuthType getAuthType() { return authType; }
    public void setAuthType(AuthType authType) { this.authType = authType; }

    public AuthType getAltAuthType() { return altAuthType; }
    public void setAltAuthType(AuthType altAuthType) { this.altAuthType = altAuthType; }

    public List<Map<String, Object>> getTriggers() { return triggers; }
    public void setTriggers(List<Map<String, Object>> triggers) { this.triggers = triggers; }

    public List<Map<String, Object>> getActions() { return actions; }
    public void setActions(List<Map<String, Object>> actions) { this.actions = actions; }

    public List<Map<String, Object>> getCredentialSchema() { return credentialSchema; }
    public void setCredentialSchema(List<Map<String, Object>> credentialSchema) { this.credentialSchema = credentialSchema; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getHelpUrl() { return helpUrl; }
    public void setHelpUrl(String helpUrl) { this.helpUrl = helpUrl; }

    public boolean isInternal() { return internal; }
    public void setInternal(boolean internal) { this.internal = internal; }

    public boolean isHasPlatformKey() { return hasPlatformKey; }
    public void setHasPlatformKey(boolean hasPlatformKey) { this.hasPlatformKey = hasPlatformKey; }
}
