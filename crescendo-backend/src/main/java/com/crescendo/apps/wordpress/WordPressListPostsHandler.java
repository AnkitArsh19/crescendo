package com.crescendo.apps.wordpress;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import tools.jackson.databind.ObjectMapper;

@ActionMapping(appKey = "wordpress", actionKey = "list-posts")
public class WordPressListPostsHandler extends WordPressHandler {

    public WordPressListPostsHandler(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
public ActionResult execute(ActionContext context) {
        int perPage = Math.max(1, Math.min(100, intValue(context.configuration().get("perPage"), 10)));
        String search = value(context, "search", "");
        if (search.isBlank()) {
            return get(context, "/wp-json/wp/v2/posts?per_page={perPage}", perPage);
        }
        return get(context, "/wp-json/wp/v2/posts?per_page={perPage}&search={search}", perPage, search);
    }
}
