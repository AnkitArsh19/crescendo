package com.crescendo.execution.resource;

/**
 * A single selectable item returned by a {@link ResourceProvider}.
 * <p>
 * Rendered as an option in a dynamic dropdown on the workflow configuration panel.
 *
 * @param id          provider-native identifier (e.g. spreadsheet ID, channel ID)
 * @param label       human-readable display name (e.g. "Monthly Sales Template")
 * @param description optional subtitle or metadata (e.g. "ID: 1gCaqox…")
 */
public record ResourceOption(
        String id,
        String label,
        String description
) {

    /**
     * Convenience constructor when no description is needed.
     */
    public ResourceOption(String id, String label) {
        this(id, label, null);
    }
}
