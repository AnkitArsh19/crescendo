package com.crescendo.apps.rss;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionHandler;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@ActionMapping(appKey = "rss", actionKey = "read-feed")
public class RssReadFeedHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RssReadFeedHandler.class);

    @Override
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();

        String feedUrl = config.get("feedUrl") != null ? config.get("feedUrl").toString() : null;
        if (feedUrl == null || feedUrl.isBlank()) {
            return ActionResult.failure("'feedUrl' is required");
        }

        try {
            String xmlContent = RestClient.create()
                    .get()
                    .uri(feedUrl)
                    .retrieve()
                    .body(String.class);

            Map<String, Object> output = new HashMap<>();
            output.put("feedUrl", feedUrl);
            output.put("rawXml", xmlContent);
            logger.info("[rss] Feed read successfully, url={}", feedUrl);
            return ActionResult.success(output);
        } catch (Exception e) {
            logger.error("[rss] Read feed failed: {}", feedUrl, e);
            return ActionResult.failure("RSS read feed failed: " + e.getMessage());
        }
    }
}
