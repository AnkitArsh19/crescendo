package com.crescendo.apps.instagram;

import com.crescendo.apps.simpleapi.SimpleApiSupport;
import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import java.util.*;

class IgBase {
    static RestClient c(ActionContext x) {
        String v = SimpleApiSupport.cred(x, "graphVersion");
        return RestClient.create("https://graph.facebook.com/" + (v.isBlank() ? "v20.0" : v));
    }
}

@ActionMapping(appKey = "instagram", actionKey = "create-media-container")
class InstagramCreateMediaContainerHandler implements ActionHandler {
    private final ObjectMapper m;

    InstagramCreateMediaContainerHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("image_url", SimpleApiSupport.cfg(c, "imageUrl"));
            body.put("caption", SimpleApiSupport.cfg(c, "caption"));
            body.put("access_token", SimpleApiSupport.cred(c, "accessToken"));
            String res = IgBase.c(c).post()
                    .uri("/{igUserId}/media", SimpleApiSupport.cfg(c, "igUserId"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return SimpleApiSupport.parsed(m, res);
        } catch (Exception e) {
            return ActionResult.failure("Instagram create media container failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "instagram", actionKey = "publish-media")
class InstagramPublishMediaHandler implements ActionHandler {
    private final ObjectMapper m;

    InstagramPublishMediaHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String res = IgBase.c(c).post()
                    .uri("/{igUserId}/media_publish", SimpleApiSupport.cfg(c, "igUserId"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "creation_id", SimpleApiSupport.cfg(c, "creationId"),
                            "access_token", SimpleApiSupport.cred(c, "accessToken")
                    ))
                    .retrieve()
                    .body(String.class);
            return SimpleApiSupport.parsed(m, res);
        } catch (Exception e) {
            return ActionResult.failure("Instagram publish media failed: " + e.getMessage());
        }
    }
}
