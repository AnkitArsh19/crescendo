package com.crescendo.app;

import com.crescendo.enums.AuthType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "app",
    indexes = {
        @Index(name = "idx_app_name", columnList = "name")
    })
public class App {

    @Id
    @Column(name = "appKey", nullable = false)
    private String appKey;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "logoUrl")
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "authType", nullable = false)
    private AuthType authType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "triggers", columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> triggers;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actions", columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> actions;

    public App() {
    }

    public App(String appKey, String name, String description, String logoUrl, AuthType authType, List<Map<String, Object>> triggers, List<Map<String, Object>> actions) {
        this.appKey = appKey;
        this.name = name;
        this.description = description;
        this.logoUrl = logoUrl;
        this.authType = authType;
        this.triggers = triggers;
        this.actions = actions;
    }

    public String getAppKey() {
        return appKey;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public List<Map<String, Object>> getTriggers() {
        return triggers;
    }

    public List<Map<String, Object>> getActions() {
        return actions;
    }
}
