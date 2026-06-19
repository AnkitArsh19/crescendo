package com.crescendo.apps.git;

import com.crescendo.execution.action.*;
import java.util.*;

@ActionMapping(appKey = "git", actionKey = "log")
public class GitLogHandler extends GitCliHandler {
    @Override
    public ActionResult execute(ActionContext c) {
        return run(c, List.of("log", "--oneline", "-n", String.valueOf(Math.max(1, lim(c)))));
    }
}
