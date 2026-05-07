package com.crescendo.enums;

/**
 * Status of a user's connection to an external app/service.
 */
public enum ConnectionStatus {
    ACTIVE,     // Connection is valid and working
    ERROR,      // Connection encountered an error
    REAUTH      // User needs to re-authenticate
}