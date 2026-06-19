package com.crescendo.apps.htmlextract;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ActionMapping(appKey = "html-extract", actionKey = "extract")
public class HtmlExtractHandler implements ActionHandler {
    @Override
public ActionResult execute(ActionContext context) {
        String html = str(context.configuration().get("html"));
        String selector = str(context.configuration().get("selector")).trim();
        String attribute = str(context.configuration().get("attribute")).trim();
        if (html.isBlank() || selector.isBlank()) return ActionResult.failure("HTML Extract requires html and selector");
        try {
            List<String> matches = extract(html, selector, attribute);
            return ActionResult.success(Map.of("matches", matches, "first", matches.isEmpty() ? "" : matches.get(0), "count", matches.size()));
        } catch (Exception e) {
            return ActionResult.failure("HTML Extract failed: " + e.getMessage());
        }
    }

    private List<String> extract(String html, String selector, String attribute) {
        String tag = "[a-zA-Z][a-zA-Z0-9:-]*";
        String predicate = "";
        if (selector.startsWith("#")) {
            predicate = "(?=[^>]*\\bid\\s*=\\s*['\"]" + Pattern.quote(selector.substring(1)) + "['\"])";
        } else if (selector.startsWith(".")) {
            predicate = "(?=[^>]*\\bclass\\s*=\\s*['\"][^'\"]*\\b" + Pattern.quote(selector.substring(1)) + "\\b[^'\"]*['\"])";
        } else {
            tag = Pattern.quote(selector);
        }
        Pattern p = Pattern.compile("<(" + tag + ")" + predicate + "\\b([^>]*)>(.*?)</\\1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(html);
        List<String> out = new ArrayList<>();
        while (m.find()) {
            if (!attribute.isBlank()) {
                Matcher attr = Pattern.compile("\\b" + Pattern.quote(attribute) + "\\s*=\\s*['\"]([^'\"]*)['\"]", Pattern.CASE_INSENSITIVE).matcher(m.group(2));
                if (attr.find()) out.add(attr.group(1));
            } else {
                out.add(m.group(3).replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim());
            }
        }
        return out;
    }

    private String str(Object v) { return v == null ? "" : String.valueOf(v); }
}
