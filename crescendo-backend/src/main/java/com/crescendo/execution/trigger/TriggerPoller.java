package com.crescendo.execution.trigger;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Strategy interface for polling-based triggers.
 *
 * Each implementation knows how to check a specific app's API for new events
 * (e.g., new emails in Gmail, new messages in Outlook) and returns any
 * items that arrived since the last poll.
 *
 * Implementations are auto-discovered by Spring and registered with
 * {@link PollingTriggerScheduler}.
 */
public interface TriggerPoller {

    /**
     * Returns true if this poller handles the given app + trigger combination.
     *
     * @param appKey     the app key (e.g., "gmail", "microsoft-outlook")
     * @param triggerKey the trigger key (e.g., "new-email")
     */
    boolean supports(String appKey, String triggerKey);

    /**
     * Polls the external API for new events since the given timestamp.
     *
     * @param credentials   decrypted OAuth credentials (contains "accessToken")
     * @param configuration step configuration from the user (e.g., subject filter, sender filter)
     * @param lastPollTime  the last time this step was polled; only return events after this time
     * @return list of trigger payloads — each map represents one event that should start a workflow run
     */
    List<Map<String, Object>> poll(Map<String, Object> credentials,
                                   Map<String, Object> configuration,
                                   Instant lastPollTime);
}
