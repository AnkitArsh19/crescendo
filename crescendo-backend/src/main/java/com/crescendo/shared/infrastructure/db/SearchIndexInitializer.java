package com.crescendo.shared.infrastructure.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Initializes PostgreSQL specific extensions and indexes on application startup.
 * Instead of introducing Elasticsearch, we use Postgres native tsvector and pg_trgm.
 */
@Component
public class SearchIndexInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(SearchIndexInitializer.class);
    private final JdbcTemplate jdbcTemplate;

    public SearchIndexInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        logger.info("Initializing Postgres Full-Text Search extensions and indexes...");
        try {
            // Enable fuzzy matching extension
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");

            // EmailLog full-text search
            jdbcTemplate.execute("""
                ALTER TABLE email_log 
                ADD COLUMN IF NOT EXISTS search_vector tsvector 
                GENERATED ALWAYS AS (
                    to_tsvector('english', coalesce(subject, '') || ' ' || coalesce(error, ''))
                ) STORED
            """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS email_log_search_idx ON email_log USING GIN(search_vector)");

            // WorkflowRun full-text search
            jdbcTemplate.execute("""
                ALTER TABLE workflow_run 
                ADD COLUMN IF NOT EXISTS search_vector tsvector 
                GENERATED ALWAYS AS (
                    to_tsvector('english', coalesce(error_message, ''))
                ) STORED
            """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS workflow_run_search_idx ON workflow_run USING GIN(search_vector)");

            logger.info("Postgres Full-Text Search initialized successfully.");
        } catch (Exception e) {
            logger.warn("Failed to initialize Postgres search indexes (this is normal if running tests without Postgres): {}", e.getMessage());
        }
    }
}
