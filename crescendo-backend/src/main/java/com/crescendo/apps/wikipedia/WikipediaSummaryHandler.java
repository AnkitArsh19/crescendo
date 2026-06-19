package com.crescendo.apps.wikipedia;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;

@ActionMapping(appKey = "wikipedia", actionKey = "get-summary")
public class WikipediaSummaryHandler extends WikipediaHandler {

    public WikipediaSummaryHandler(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
public ActionResult execute(ActionContext context) {
        String title = value(context, "title", "");
        if (title.isBlank()) return ActionResult.failure("Wikipedia title is required");
        String language = value(context, "language", "en");
        return get(language, "/api/rest_v1/page/summary/{title}", title.replace(' ', '_'));
    }
}
