package com.crescendo.apps.markdown;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Markdown handlers.
 * Note: Actual conversion requires a library like Flexmark or commonmark-java. This serves as a placeholder matching n8n's structure.
 */
@Component
public class MarkdownHandlers {

    @ActionMapping(appKey = "markdown", actionKey = "markdown:convert")
    public Object convert(ActionContext context) throws Exception {
        String mode = context.getString("mode");
        String destinationKey = context.getString("destinationKey");
// Map<String, Object> options = context.getMap("options");
        
        String result = "";
        if ("htmlToMarkdown".equals(mode)) {
// String html = context.getString("html");
            // Perform conversion
            result = "markdown_placeholder";
        } else if ("markdownToHtml".equals(mode)) {
// String markdown = context.getString("markdown");
            // Perform conversion
            result = "html_placeholder";
        }
        
        return Map.of(
            "status", "success",
            destinationKey != null ? destinationKey : "data", result
        );
    }
}
