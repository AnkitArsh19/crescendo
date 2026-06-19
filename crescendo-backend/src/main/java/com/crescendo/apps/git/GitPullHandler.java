package com.crescendo.apps.git;

import com.crescendo.execution.action.*;
import java.util.*;

@ActionMapping(appKey = "git", actionKey = "pull")
public class GitPullHandler extends GitCliHandler {
    @Override
    public ActionResult execute(ActionContext c) {
        return run(c, List.of("pull", "--ff-only"));
    }
}
