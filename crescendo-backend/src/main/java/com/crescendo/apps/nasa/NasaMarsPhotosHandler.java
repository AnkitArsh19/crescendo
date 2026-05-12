package com.crescendo.apps.nasa;

import com.crescendo.execution.action.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import java.util.*;

/**
 * Gets Mars Rover photos via NASA Mars Photos API.
 */
@ActionMapping(appKey = "nasa-apod", actionKey = "get-mars-photos")
public class NasaMarsPhotosHandler implements ActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(NasaMarsPhotosHandler.class);
    private final RestClient restClient = RestClient.create();

    @Override
    @SuppressWarnings("unchecked")
    public ActionResult execute(ActionContext context) {
        Map<String, Object> config = context.configuration();
        String rover = config.getOrDefault("rover", "curiosity").toString().toLowerCase();
        String sol = config.getOrDefault("sol", "1000").toString();

        try {
            String url = "https://api.nasa.gov/mars-photos/api/v1/rovers/" + rover
                    + "/photos?sol=" + sol + "&api_key=DEMO_KEY";

            Map<String, Object> resp = restClient.get().uri(url).retrieve().body(Map.class);

            Map<String, Object> out = new HashMap<>();
            out.put("provider", "nasa");
            out.put("action", "get-mars-photos");
            out.put("rover", rover);
            if (resp != null && resp.containsKey("photos")) {
                var photos = (List<Map<String, Object>>) resp.get("photos");
                out.put("photoCount", photos.size());
                if (!photos.isEmpty()) {
                    out.put("firstPhotoUrl", photos.get(0).get("img_src"));
                    out.put("camera", ((Map<?,?>)photos.get(0).get("camera")).get("full_name"));
                }
            }
            return ActionResult.success(out);
        } catch (Exception e) {
            return ActionResult.failure("NASA Mars photos failed: " + e.getMessage());
        }
    }
}
