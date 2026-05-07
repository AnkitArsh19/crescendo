package com.crescendo.enums;

/**
 * Authentication types supported by apps for user connections.
 */
public enum AuthType {
    NONE,       // No auth required (webhooks, public APIs)
    OAUTH2,     // OAuth 2.0 flow
    APIKEY      // API Key based authentication
}
