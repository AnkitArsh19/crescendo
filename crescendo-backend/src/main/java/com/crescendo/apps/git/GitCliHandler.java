package com.crescendo.apps.git;

import com.crescendo.execution.action.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

abstract class GitCliHandler implements ActionHandler {
    ActionResult run(ActionContext c, List<String> args) {
        try {
            String path = String.valueOf(c.configuration().getOrDefault("repoPath", ""));
            if (path.isBlank()) {
                return ActionResult.failure("Git repoPath is required");
            }
            List<String> cmd = new ArrayList<>();
            cmd.add("git");
            cmd.addAll(args);
            Process p = new ProcessBuilder(cmd)
                    .directory(new File(path))
                    .redirectErrorStream(true)
                    .start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int code = p.waitFor();
            return ActionResult.success(Map.of("exitCode", code, "output", out));
        } catch (Exception e) {
            return ActionResult.failure("Git command failed: " + e.getMessage());
        }
    }

    int lim(ActionContext c) {
        try {
            return Integer.parseInt(String.valueOf(c.configuration().getOrDefault("limit", "5")));
        } catch (Exception e) {
            return 5;
        }
    }
}
