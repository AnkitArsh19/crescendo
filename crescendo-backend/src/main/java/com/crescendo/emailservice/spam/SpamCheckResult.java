package com.crescendo.emailservice.spam;

import java.util.List;

public record SpamCheckResult(
        boolean hasIssues,
        List<String> warnings
) {
    public static SpamCheckResult ok() {
        return new SpamCheckResult(false, List.of());
    }
}
