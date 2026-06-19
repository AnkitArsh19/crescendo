package com.crescendo.execution.action;

import java.time.Instant;
import java.util.Map;

/**
 * Thrown by an ActionHandler to pause execution of a workflow
 * until the specified resume time or external event.
 */
public class SuspendExecutionException extends RuntimeException {
    
    private final Instant resumeAt;
    private final String resumeToken;
    private final Map<String, Object> outputData;
    
    public SuspendExecutionException(Instant resumeAt, String message) {
        this(resumeAt, null, null, message);
    }

    public SuspendExecutionException(Instant resumeAt, String resumeToken, Map<String, Object> outputData, String message) {
        super(message);
        this.resumeAt = resumeAt;
        this.resumeToken = resumeToken;
        this.outputData = outputData;
    }
    
    public Instant getResumeAt() {
        return resumeAt;
    }

    public String getResumeToken() {
        return resumeToken;
    }

    public Map<String, Object> getOutputData() {
        return outputData;
    }
}
