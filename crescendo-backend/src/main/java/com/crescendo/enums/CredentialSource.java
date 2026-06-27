package com.crescendo.enums;

/**
 * Indicates where a credential comes from during step execution.
 *
 * <ul>
 *   <li>{@code PERSONAL} — the user's own connection (stored in {@code connections_command}).</li>
 *   <li>{@code PLATFORM} — an admin-configured platform key (stored in {@code platform_key}),
 *       used as a fallback when the user has not connected the app themselves.</li>
 * </ul>
 *
 * <p>The <strong>auth mechanism</strong> ({@link AuthType}) is independent of the source.
 * For example, OpenAI is always {@code APIKEY} — the only question is <em>whose</em> key is used.
 * This enum answers that question without changing {@code AuthType} or duplicating
 * {@code AppDefinition} entries.
 */
public enum CredentialSource {
    PERSONAL,
    PLATFORM
}
