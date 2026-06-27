package com.crescendo.apps.html;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ActionMapping(appKey = "html", actionKey = ".*")
@SuppressWarnings("unchecked")
public class HtmlActionHandler implements ActionHandler {

    @Override
    public ActionResult execute(ActionContext context) {
        String actionKey = context.actionKey();
        Map<String, Object> config = context.configuration();

        try {
            switch (actionKey) {
                case "generateHtmlTemplate":
                    return generateHtmlTemplate(config);
                case "extractHtmlContent":
                    return extractHtmlContent(config);
                case "convertToHtmlTable":
                    return convertToHtmlTable(config);
                default:
                    return ActionResult.failure("Unknown action: " + actionKey);
            }
        } catch (Exception e) {
            return ActionResult.failure("HTML action failed: " + e.getMessage());
        }
    }

    private ActionResult generateHtmlTemplate(Map<String, Object> config) {
        String html = String.valueOf(config.get("html"));
        return ActionResult.success(Map.of("html", html));
    }

    private ActionResult extractHtmlContent(Map<String, Object> config) {
        String html = String.valueOf(config.get("html"));
        Object extractionValuesObj = config.get("extractionValues");
        
        List<Map<String, Object>> extractionValues;
        if (extractionValuesObj instanceof List) {
            extractionValues = (List<Map<String, Object>>) extractionValuesObj;
        } else {
            return ActionResult.failure("extractionValues must be an array");
        }

        Document doc = Jsoup.parse(html);
        Map<String, Object> result = new HashMap<>();

        for (Map<String, Object> valueData : extractionValues) {
            String key = String.valueOf(valueData.getOrDefault("key", ""));
            String cssSelector = String.valueOf(valueData.getOrDefault("cssSelector", ""));
            String returnValue = String.valueOf(valueData.getOrDefault("returnValue", "text"));
            boolean returnArray = "true".equalsIgnoreCase(String.valueOf(valueData.get("returnArray")));

            Elements elements = doc.select(cssSelector);
            if (returnArray) {
                List<String> values = new ArrayList<>();
                for (Element el : elements) {
                    values.add(extractValue(el, valueData, returnValue));
                }
                result.put(key, values);
            } else {
                if (!elements.isEmpty()) {
                    result.put(key, extractValue(elements.first(), valueData, returnValue));
                } else {
                    result.put(key, null);
                }
            }
        }

        return ActionResult.success(result);
    }

    private String extractValue(Element el, Map<String, Object> valueData, String returnValue) {
        if ("attribute".equals(returnValue)) {
            String attr = String.valueOf(valueData.getOrDefault("attribute", ""));
            return el.attr(attr).trim();
        } else if ("html".equals(returnValue)) {
            return el.html().trim();
        } else if ("value".equals(returnValue)) {
            return el.val().trim();
        } else {
            return el.text().trim();
        }
    }

    private ActionResult convertToHtmlTable(Map<String, Object> config) {
        Object dataObj = config.get("data");
        if (!(dataObj instanceof List)) {
            return ActionResult.failure("data must be an array of objects");
        }

        List<Map<String, Object>> data = (List<Map<String, Object>>) dataObj;
        if (data.isEmpty()) {
            return ActionResult.success(Map.of("html", "<table></table>"));
        }

        StringBuilder html = new StringBuilder();
        html.append("<table>\\n");
        
        // Extract headers from the first object
        Map<String, Object> firstRow = data.get(0);
        List<String> headers = new ArrayList<>(firstRow.keySet());
        
        html.append("  <thead>\\n    <tr>\\n");
        for (String header : headers) {
            html.append("      <th>").append(header).append("</th>\\n");
        }
        html.append("    </tr>\\n  </thead>\\n");
        
        html.append("  <tbody>\\n");
        for (Map<String, Object> row : data) {
            html.append("    <tr>\\n");
            for (String header : headers) {
                Object value = row.get(header);
                html.append("      <td>").append(value != null ? value.toString() : "").append("</td>\\n");
            }
            html.append("    </tr>\\n");
        }
        html.append("  </tbody>\\n");
        html.append("</table>");

        return ActionResult.success(Map.of("html", html.toString()));
    }
}
