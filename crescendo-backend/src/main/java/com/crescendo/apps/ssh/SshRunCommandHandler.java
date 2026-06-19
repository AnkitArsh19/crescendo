package com.crescendo.apps.ssh;

import com.crescendo.execution.action.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@ActionMapping(appKey = "ssh", actionKey = "run-command")
public class SshRunCommandHandler implements ActionHandler {
    @Override
    public ActionResult execute(ActionContext c) {
        try {
            String cmd = String.valueOf(c.configuration().getOrDefault("command", ""));
            if (cmd.isBlank()) {
                return ActionResult.failure("SSH command is required");
            }
            List<String> ssh = new ArrayList<>(List.of("ssh", "-o", "BatchMode=yes", "-p", cred(c, "port", "22")));
            String key = cred(c, "identityFile", "");
            if (!key.isBlank()) {
                ssh.add("-i");
                ssh.add(key);
            }
            ssh.add(cred(c, "username", "") + "@" + cred(c, "host", ""));
            ssh.add(cmd);
            Process p = new ProcessBuilder(ssh).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int code = p.waitFor();
            return ActionResult.success(Map.of("exitCode", code, "output", out));
        } catch (Exception e) {
            return ActionResult.failure("SSH command failed: " + e.getMessage());
        }
    }

    private String cred(ActionContext c, String k, String f) {
        Object v = c.credentials() != null ? c.credentials().get(k) : null;
        return v == null ? f : String.valueOf(v);
    }
}
