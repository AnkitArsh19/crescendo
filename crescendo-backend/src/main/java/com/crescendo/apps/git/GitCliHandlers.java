package com.crescendo.apps.git;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;

@Component
public class GitCliHandlers {

    private String executeCommand(String repoPath, String... commands) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.directory(new File(repoPath));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed with exit code " + exitCode + ":\n" + output);
        }
        return output.toString();
    }

    @ActionMapping(appKey = "git", actionKey = "status")
    public Object status(ActionContext context) throws Exception {
        String repoPath = context.configuration().get("repoPath") != null ? context.configuration().get("repoPath").toString() : "";
        if (repoPath.isBlank()) return ActionResult.failure("Repository Path is required");
        
        try {
            String output = executeCommand(repoPath, "git", "status");
            return ActionResult.success(Map.of("data", output));
        } catch (Exception e) {
            return ActionResult.failure("Failed to run git status: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "git", actionKey = "pull")
    public Object pull(ActionContext context) throws Exception {
        String repoPath = context.configuration().get("repoPath") != null ? context.configuration().get("repoPath").toString() : "";
        if (repoPath.isBlank()) return ActionResult.failure("Repository Path is required");
        
        try {
            String output = executeCommand(repoPath, "git", "pull");
            return ActionResult.success(Map.of("data", output));
        } catch (Exception e) {
            return ActionResult.failure("Failed to run git pull: " + e.getMessage());
        }
    }

    @ActionMapping(appKey = "git", actionKey = "log")
    public Object log(ActionContext context) throws Exception {
        String repoPath = context.configuration().get("repoPath") != null ? context.configuration().get("repoPath").toString() : "";
        if (repoPath.isBlank()) return ActionResult.failure("Repository Path is required");

        String limit = context.configuration().get("limit") != null ? context.configuration().get("limit").toString() : "5";
        
        try {
            String output = executeCommand(repoPath, "git", "log", "-n", limit, "--oneline");
            return ActionResult.success(Map.of("data", output));
        } catch (Exception e) {
            return ActionResult.failure("Failed to run git log: " + e.getMessage());
        }
    }
}
