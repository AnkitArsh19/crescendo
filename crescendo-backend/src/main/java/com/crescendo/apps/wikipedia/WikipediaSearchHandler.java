package com.crescendo.apps.wikipedia;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;

@ActionMapping(appKey = "wikipedia", actionKey = "search")
public class WikipediaSearchHandler extends WikipediaHandler {

    public WikipediaSearchHandler(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
public ActionResult execute(ActionContext context) {
        String query = value(context, "query", "");
        if (query.isBlank()) return ActionResult.failure("Wikipedia query is required");
        String language = value(context, "language", "en");
        int limit = intValue(context.configuration().get("limit"), 10);
        return get(language, "/w/rest.php/v1/search/page?q={query}&limit={limit}", query, Math.max(1, Math.min(50, limit)));
    }

    private int intValue(Object value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
