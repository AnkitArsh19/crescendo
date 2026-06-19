package com.crescendo.apps.facebookgraph;

import com.crescendo.apps.simpleapi.SimpleApiSupport;
import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import java.util.*;

class FbBase {
    static String base(ActionContext c) {
        String v = SimpleApiSupport.cred(c, "graphVersion");
        return "https://graph.facebook.com/" + (v.isBlank() ? "v20.0" : v);
    }

    static RestClient client(ActionContext c) {
        return RestClient.create(base(c));
    }
}

@ActionMapping(appKey = "facebook-graph", actionKey = "get-node")
class FacebookGetNodeHandler implements ActionHandler {
    private final ObjectMapper m;

    FacebookGetNodeHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String fields = SimpleApiSupport.cfg(c, "fields");
            String uri = fields.isBlank() ? "/{id}?access_token={token}" : "/{id}?fields={fields}&access_token={token}";
            String res = fields.isBlank() ?
                    FbBase.client(c).get().uri(uri, SimpleApiSupport.cfg(c, "nodeId"), SimpleApiSupport.cred(c, "accessToken"))
                            .retrieve().body(String.class) :
                    FbBase.client(c).get().uri(uri, SimpleApiSupport.cfg(c, "nodeId"), fields, SimpleApiSupport.cred(c, "accessToken"))
                            .retrieve().body(String.class);
            return SimpleApiSupport.parsed(m, res);
        } catch (Exception e) {
            return ActionResult.failure("Facebook get node failed: " + e.getMessage());
        }
    }
}

@ActionMapping(appKey = "facebook-graph", actionKey = "create-page-post")
class FacebookCreatePagePostHandler implements ActionHandler {
    private final ObjectMapper m;

    FacebookCreatePagePostHandler(ObjectMapper m) {
        this.m = m;
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String res = FbBase.client(c).post()
                    .uri("/{pageId}/feed?access_token={token}", SimpleApiSupport.cfg(c, "pageId"), SimpleApiSupport.cred(c, "accessToken"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", SimpleApiSupport.cfg(c, "message")))
                    .retrieve()
                    .body(String.class);
            return SimpleApiSupport.parsed(m, res);
        } catch (Exception e) {
            return ActionResult.failure("Facebook create page post failed: " + e.getMessage());
        }
    }
}
