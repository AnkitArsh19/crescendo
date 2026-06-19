package com.crescendo.apps.jenkins;

import com.crescendo.execution.action.*;
import tools.jackson.databind.ObjectMapper;
import java.util.*;

@ActionMapping(appKey = "jenkins", actionKey = "get-job")
public class JenkinsGetJobHandler extends JenkinsHandler {
    public JenkinsGetJobHandler(ObjectMapper m) {
        super(m);
    }

    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String res = client(c).get().uri(path(c) + "/api/json").retrieve().body(String.class);
            return ActionResult.success(Map.of("data", mapper.readValue(res, Object.class), "raw", res));
        } catch (Exception e) {
            return ActionResult.failure("Jenkins get job failed: " + e.getMessage());
        }
    }
}
