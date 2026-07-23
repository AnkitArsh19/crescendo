package com.crescendo.execution.resource;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contract for apps that expose selectable resources from a user's connected account.
 * <p>
 * Implementations are discovered automatically via Spring component scanning.
 * The {@link ResourceProviderRegistry} indexes them by {@link #appKey()}.
 * <p>
 * <b>Example:</b> A Google Sheets provider might support three resource types:
 * <ul>
 *   <li>{@code "spreadsheets"} — list all spreadsheets in the user's Drive</li>
 *   <li>{@code "worksheets"} — list sheets within a specific spreadsheet (requires {@code spreadsheetId} param)</li>
 *   <li>{@code "columns"} — list header columns (requires {@code spreadsheetId} + {@code sheetName} params)</li>
 * </ul>
 *
 * <p><b>To add resource fetching to an existing app:</b>
 * <ol>
 *   <li>Create a class implementing {@code ResourceProvider} in the app's package</li>
 *   <li>Annotate with {@code @Component}</li>
 *   <li>Update the app's {@code configSchema} to use {@code type: "dynamic_dropdown"}</li>
 *   <li>That's it — auto-discovered, no registry changes needed</li>
 * </ol>
 *
 * @see ResourceProviderRegistry
 * @see ResourceFetchService
 */
public interface ResourceProvider {

    /**
     * The app key this provider belongs to (must match the {@code AppDefinition.toApp().getAppKey()}).
     */
    String appKey();

    /**
     * Fetches a list of selectable resources from the external service.
     *
     * @param credentials  decrypted connection credentials (e.g. {@code accessToken}, {@code botToken})
     * @param resourceType the kind of resource to list (e.g. {@code "channels"}, {@code "spreadsheets"})
     * @param params       parent selections for cascading lookups
     *                     (e.g. {@code {"spreadsheetId": "abc123"}} when fetching worksheets)
     * @return ordered list of options; empty list if none found or if the resource type is unsupported
     */
    List<ResourceOption> listResources(
            Map<String, Object> credentials,
            String resourceType,
            Map<String, String> params
    );

    /**
     * Resource types this provider can handle.
     * Used for validation and API documentation.
     */
    Set<String> supportedResourceTypes();

    /** Zero-parameter resources that are safe to snapshot for workflow planning. */
    default Set<ResourceContextDescriptor> contextResourceDescriptors() {
        return Set.of();
    }
}
