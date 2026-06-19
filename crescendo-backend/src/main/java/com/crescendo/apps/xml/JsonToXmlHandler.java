package com.crescendo.apps.xml;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ActionMapping(appKey = "xml", actionKey = "to-xml")
public class JsonToXmlHandler implements ActionHandler {
    @Override
public ActionResult execute(ActionContext context) {
        Object data = context.configuration().get("data");
        String rootName = String.valueOf(context.configuration().getOrDefault("rootName", "root"));
        if (!(data instanceof Map<?, ?> map)) {
            return ActionResult.failure("JSON to XML requires data to be an object");
        }
        return ActionResult.success(Map.of("xml", element(rootName, map)));
    }

    private String element(String name, Object value) {
        if (value instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("<").append(escapeName(name)).append(">");
            map.forEach((key, childValue) -> sb.append(element(String.valueOf(key), childValue)));
            return sb.append("</").append(escapeName(name)).append(">").toString();
        }
        return "<" + escapeName(name) + ">" + escapeText(value) + "</" + escapeName(name) + ">";
    }

    private String escapeName(String name) {
        return name.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private String escapeText(Object value) {
        return String.valueOf(value == null ? "" : value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
