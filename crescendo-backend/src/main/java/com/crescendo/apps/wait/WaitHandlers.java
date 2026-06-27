package com.crescendo.apps.wait;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Wait handlers.
 * Note: Actual execution suspending requires the workflow engine to handle state persisting and resuming. This serves as a placeholder matching n8n's structure.
 */
@Component
public class WaitHandlers {

    @ActionMapping(appKey = "wait", actionKey = "wait:wait")
    public Object wait(ActionContext context) throws Exception {
        String resume = context.getString("resume");
        
        // Return a response that indicates execution should be put to wait
        // Real implementation would interact with the workflow engine
        
        return Map.of(
            "status", "waiting",
            "resume_mode", resume != null ? resume : "timeInterval",
            "message", "Execution put to wait"
        );
    }
}
