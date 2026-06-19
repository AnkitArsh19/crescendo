package com.crescendo.apps.jenkins;

import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import java.util.*;

@ActionMapping(appKey = "jenkins", actionKey = "trigger-build")
public class JenkinsTriggerBuildHandler extends JenkinsHandler {
    public JenkinsTriggerBuildHandler(ObjectMapper m) {
        super(m);
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            Object p = json(c.configuration().get("parameters"), Map.of());
            String uri = path(c) + (((p instanceof Map<?, ?> map) && !map.isEmpty()) ? "/buildWithParameters" : "/build");
            ResponseEntity<String> r = client(c).post().uri(uri).retrieve().toEntity(String.class);
            return ActionResult.success(Map.of("status", r.getStatusCode().value(), "location", String.valueOf(r.getHeaders().getFirst("Location"))));
        } catch (Exception e) {
            return ActionResult.failure("Jenkins trigger failed: " + e.getMessage());
        }
    }
}
