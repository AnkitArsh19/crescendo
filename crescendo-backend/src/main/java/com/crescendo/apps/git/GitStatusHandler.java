package com.crescendo.apps.git;

import com.crescendo.execution.action.*;
import java.util.*;

@ActionMapping(appKey = "git", actionKey = "status")
public class GitStatusHandler extends GitCliHandler {
    @Override
    public ActionResult execute(ActionContext c) {
        return run(c, List.of("status", "--short"));
    }
}
