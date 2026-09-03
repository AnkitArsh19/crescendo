-- Initialize CQRS databases if not existing
SELECT 'CREATE DATABASE crescendo_query'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'crescendo_query')\gexec
