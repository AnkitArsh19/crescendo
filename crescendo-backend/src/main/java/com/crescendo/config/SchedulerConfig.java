package com.crescendo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's {@code @Scheduled} annotation support.
 * Required for the polling trigger scheduler to run periodically.
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
}
