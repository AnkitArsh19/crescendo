package com.crescendo.apps.wordpress;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@ActionMapping(appKey = "wordpress", actionKey = "create-post")
public class WordPressCreatePostHandler extends WordPressHandler {

    public WordPressCreatePostHandler(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
public ActionResult execute(ActionContext context) {
        String title = value(context, "title", "");
        String content = value(context, "content", "");
        if (title.isBlank() || content.isBlank()) {
            return ActionResult.failure("WordPress title and content are required");
        }
        return post(context, Map.of(
                "title", title,
                "content", content,
                "status", value(context, "status", "draft")
        ));
    }
}
