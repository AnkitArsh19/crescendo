package com.crescendo.apps.markdown;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ActionMapping(appKey = "markdown", actionKey = "to-html")
public class MarkdownActionHandler implements ActionHandler {

    @Override
public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        
        if (config == null || !config.containsKey("text")) {
            return ActionResult.failure("Markdown to HTML action requires 'text' in configuration");
        }
        
        String markdown = String.valueOf(config.get("text"));
        
        try {
            Parser parser = Parser.builder().build();
            Node document = parser.parse(markdown);
            HtmlRenderer renderer = HtmlRenderer.builder().build();
            String html = renderer.render(document);
            
            return ActionResult.success(Map.of("html", html));
        } catch (Exception e) {
            return ActionResult.failure("Failed to render markdown to HTML: " + e.getMessage());
        }
    }
}
